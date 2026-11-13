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
    conn = get_connection()
    cursor = conn.cursor(dictionary=True)
    cursor.execute(
        "SELECT id, name, price, stock_quantity, low_stock_threshold "
        "FROM products WHERE is_active = TRUE"
    )
    items = cursor.fetchall()
    cursor.close()
    conn.close()
    return render_template("products.html", products=items)


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