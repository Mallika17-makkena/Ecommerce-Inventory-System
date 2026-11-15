import os
from functools import wraps
from flask import Flask, render_template, request, redirect, url_for, session, flash
import mysql.connector
from dotenv import load_dotenv
from werkzeug.security import generate_password_hash, check_password_hash

load_dotenv()

app = Flask(__name__)
app.secret_key = os.getenv("SECRET_KEY")


def get_connection():
    return mysql.connector.connect(
        host=os.getenv("DB_HOST"),
        user=os.getenv("DB_USER"),
        password=os.getenv("DB_PASSWORD"),
        database=os.getenv("DB_NAME"),
    )


def admin_required(f):
    @wraps(f)
    def wrapper(*args, **kwargs):
        if session.get("role") != "admin":
            flash("Admin access only. Please log in as admin.", "error")
            return redirect(url_for("login"))
        return f(*args, **kwargs)
    return wrapper


def login_required(f):
    @wraps(f)
    def wrapper(*args, **kwargs):
        if "user_id" not in session:
            flash("Please log in first.", "error")
            return redirect(url_for("login"))
        return f(*args, **kwargs)
    return wrapper


def get_or_create_cart(cursor, conn, user_id):
    cursor.execute("SELECT id FROM cart WHERE user_id = %s", (user_id,))
    row = cursor.fetchone()
    if row:
        return row["id"]
    cursor.execute("INSERT INTO cart (user_id) VALUES (%s)", (user_id,))
    conn.commit()
    return cursor.lastrowid


@app.route("/")
def home():
    return render_template("home.html")


@app.route("/register", methods=["GET", "POST"])
def register():
    if request.method == "POST":
        full_name = request.form["full_name"].strip()
        email = request.form["email"].strip().lower()
        password = request.form["password"]

        if len(password) < 6:
            flash("Password must be at least 6 characters.", "error")
            return render_template("register.html")

        conn = get_connection()
        cursor = conn.cursor(dictionary=True)
        cursor.execute("SELECT id FROM users WHERE email = %s", (email,))
        if cursor.fetchone():
            cursor.close()
            conn.close()
            flash("This email is already registered.", "error")
            return render_template("register.html")

        password_hash = generate_password_hash(password)
        cursor.execute(
            "INSERT INTO users (full_name, email, password_hash) VALUES (%s, %s, %s)",
            (full_name, email, password_hash),
        )
        conn.commit()
        cursor.close()
        conn.close()

        flash("Registration successful! Please log in.", "success")
        return redirect(url_for("login"))

    return render_template("register.html")


@app.route("/login", methods=["GET", "POST"])
def login():
    if request.method == "POST":
        email = request.form["email"].strip().lower()
        password = request.form["password"]

        conn = get_connection()
        cursor = conn.cursor(dictionary=True)
        cursor.execute("SELECT * FROM users WHERE email = %s", (email,))
        user = cursor.fetchone()
        cursor.close()
        conn.close()

        if user and check_password_hash(user["password_hash"], password):
            session["user_id"] = user["id"]
            session["user_name"] = user["full_name"]
            session["role"] = user["role"]
            return redirect(url_for("dashboard"))

        flash("Invalid email or password.", "error")

    return render_template("login.html")


@app.route("/dashboard")
def dashboard():
    if "user_id" not in session:
        flash("Please log in first.", "error")
        return redirect(url_for("login"))
    return render_template("dashboard.html")


@app.route("/logout")
def logout():
    session.clear()
    return redirect(url_for("login"))


@app.route("/products")
def products():
    search = request.args.get("search", "").strip()
    category_id = request.args.get("category", "").strip()

    query = """SELECT p.id, p.name, p.price, p.stock_quantity,
                      p.low_stock_threshold, c.name AS category_name
               FROM products p
               LEFT JOIN categories c ON p.category_id = c.id
               WHERE p.is_active = TRUE"""
    params = []

    if search:
        query += " AND p.name LIKE %s"
        params.append(f"%{search}%")
    if category_id:
        query += " AND p.category_id = %s"
        params.append(category_id)

    query += " ORDER BY p.id"

    conn = get_connection()
    cursor = conn.cursor(dictionary=True)
    cursor.execute(query, params)
    items = cursor.fetchall()
    cursor.execute("SELECT id, name FROM categories ORDER BY name")
    categories = cursor.fetchall()
    cursor.close()
    conn.close()

    return render_template(
        "products.html",
        products=items,
        categories=categories,
        search=search,
        selected_category=category_id,
    )


# ---------------- Cart ----------------

@app.route("/cart")
@login_required
def view_cart():
    conn = get_connection()
    cursor = conn.cursor(dictionary=True)
    cart_id = get_or_create_cart(cursor, conn, session["user_id"])
    cursor.execute(
        """SELECT ci.id AS item_id, ci.quantity, p.id AS product_id, p.name,
                  p.price, p.stock_quantity,
                  (ci.quantity * p.price) AS line_total
           FROM cart_items ci
           JOIN products p ON ci.product_id = p.id
           WHERE ci.cart_id = %s AND p.is_active = TRUE""",
        (cart_id,),
    )
    items = cursor.fetchall()
    cursor.close()
    conn.close()
    total = sum(i["line_total"] for i in items)
    return render_template("cart.html", items=items, total=total)


@app.route("/cart/add/<int:product_id>", methods=["POST"])
@login_required
def add_to_cart(product_id):
    conn = get_connection()
    cursor = conn.cursor(dictionary=True)
    cursor.execute(
        "SELECT stock_quantity FROM products WHERE id = %s AND is_active = TRUE",
        (product_id,),
    )
    product = cursor.fetchone()
    if not product or product["stock_quantity"] <= 0:
        cursor.close()
        conn.close()
        flash("This product is out of stock.", "error")
        return redirect(url_for("products"))

    cart_id = get_or_create_cart(cursor, conn, session["user_id"])
    cursor.execute(
        "SELECT id, quantity FROM cart_items WHERE cart_id = %s AND product_id = %s",
        (cart_id, product_id),
    )
    existing = cursor.fetchone()
    if existing:
        if existing["quantity"] + 1 > product["stock_quantity"]:
            flash("Not enough stock available.", "error")
        else:
            cursor.execute(
                "UPDATE cart_items SET quantity = quantity + 1 WHERE id = %s",
                (existing["id"],),
            )
            flash("Quantity updated in cart.", "success")
    else:
        cursor.execute(
            "INSERT INTO cart_items (cart_id, product_id, quantity) VALUES (%s, %s, 1)",
            (cart_id, product_id),
        )
        flash("Added to cart.", "success")
    conn.commit()
    cursor.close()
    conn.close()
    return redirect(url_for("products"))


@app.route("/cart/update/<int:item_id>", methods=["POST"])
@login_required
def update_cart(item_id):
    try:
        quantity = int(request.form["quantity"])
    except ValueError:
        quantity = 1
    conn = get_connection()
    cursor = conn.cursor(dictionary=True)
    cart_id = get_or_create_cart(cursor, conn, session["user_id"])
    cursor.execute(
        """SELECT p.stock_quantity FROM cart_items ci
           JOIN products p ON ci.product_id = p.id
           WHERE ci.id = %s AND ci.cart_id = %s""",
        (item_id, cart_id),
    )
    row = cursor.fetchone()
    if row:
        if quantity < 1:
            cursor.execute(
                "DELETE FROM cart_items WHERE id = %s AND cart_id = %s",
                (item_id, cart_id),
            )
        elif quantity > row["stock_quantity"]:
            flash("Not enough stock available.", "error")
        else:
            cursor.execute(
                "UPDATE cart_items SET quantity = %s WHERE id = %s AND cart_id = %s",
                (quantity, item_id, cart_id),
            )
            flash("Cart updated.", "success")
        conn.commit()
    cursor.close()
    conn.close()
    return redirect(url_for("view_cart"))


@app.route("/cart/remove/<int:item_id>", methods=["POST"])
@login_required
def remove_from_cart(item_id):
    conn = get_connection()
    cursor = conn.cursor(dictionary=True)
    cart_id = get_or_create_cart(cursor, conn, session["user_id"])
    cursor.execute(
        "DELETE FROM cart_items WHERE id = %s AND cart_id = %s", (item_id, cart_id)
    )
    conn.commit()
    cursor.close()
    conn.close()
    flash("Item removed.", "success")
    return redirect(url_for("view_cart"))


# ---------------- Admin products ----------------

@app.route("/admin/products")
@admin_required
def admin_products():
    conn = get_connection()
    cursor = conn.cursor(dictionary=True)
    cursor.execute(
        """SELECT p.id, p.name, p.price, p.stock_quantity,
                  p.low_stock_threshold, c.name AS category_name
           FROM products p
           LEFT JOIN categories c ON p.category_id = c.id
           WHERE p.is_active = TRUE
           ORDER BY p.id"""
    )
    items = cursor.fetchall()
    cursor.close()
    conn.close()
    return render_template("admin_products.html", products=items)


@app.route("/admin/products/add", methods=["GET", "POST"])
@admin_required
def add_product():
    conn = get_connection()
    cursor = conn.cursor(dictionary=True)
    cursor.execute("SELECT id, name FROM categories")
    categories = cursor.fetchall()

    if request.method == "POST":
        try:
            cursor.execute(
                """INSERT INTO products
                   (category_id, name, description, sku, price,
                    stock_quantity, low_stock_threshold)
                   VALUES (%s, %s, %s, %s, %s, %s, %s)""",
                (
                    request.form["category_id"],
                    request.form["name"].strip(),
                    request.form["description"].strip(),
                    request.form["sku"].strip() or None,
                    request.form["price"],
                    request.form["stock_quantity"],
                    request.form["low_stock_threshold"],
                ),
            )
            conn.commit()
            flash("Product added successfully.", "success")
            cursor.close()
            conn.close()
            return redirect(url_for("admin_products"))
        except mysql.connector.Error as e:
            flash(f"Could not add product: {e.msg}", "error")

    cursor.close()
    conn.close()
    return render_template("product_form.html", product=None, categories=categories)


@app.route("/admin/products/edit/<int:product_id>", methods=["GET", "POST"])
@admin_required
def edit_product(product_id):
    conn = get_connection()
    cursor = conn.cursor(dictionary=True)
    cursor.execute("SELECT id, name FROM categories")
    categories = cursor.fetchall()

    if request.method == "POST":
        try:
            cursor.execute(
                """UPDATE products
                   SET category_id=%s, name=%s, description=%s, sku=%s,
                       price=%s, stock_quantity=%s, low_stock_threshold=%s
                   WHERE id=%s""",
                (
                    request.form["category_id"],
                    request.form["name"].strip(),
                    request.form["description"].strip(),
                    request.form["sku"].strip() or None,
                    request.form["price"],
                    request.form["stock_quantity"],
                    request.form["low_stock_threshold"],
                    product_id,
                ),
            )
            conn.commit()
            flash("Product updated successfully.", "success")
            cursor.close()
            conn.close()
            return redirect(url_for("admin_products"))
        except mysql.connector.Error as e:
            flash(f"Could not update product: {e.msg}", "error")

    cursor.execute("SELECT * FROM products WHERE id = %s", (product_id,))
    product = cursor.fetchone()
    cursor.close()
    conn.close()
    if not product:
        flash("Product not found.", "error")
        return redirect(url_for("admin_products"))
    return render_template("product_form.html", product=product, categories=categories)


@app.route("/admin/products/delete/<int:product_id>", methods=["POST"])
@admin_required
def delete_product(product_id):
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("UPDATE products SET is_active = FALSE WHERE id = %s", (product_id,))
    conn.commit()
    cursor.close()
    conn.close()
    flash("Product removed.", "success")
    return redirect(url_for("admin_products"))


if __name__ == "__main__":
    app.run(debug=True)