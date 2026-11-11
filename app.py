import os
from flask import Flask, render_template
import mysql.connector
from dotenv import load_dotenv

load_dotenv()

app = Flask(__name__)

def get_connection():
    return mysql.connector.connect(
        host=os.getenv("DB_HOST"),
        user=os.getenv("DB_USER"),
        password=os.getenv("DB_PASSWORD"),
        database=os.getenv("DB_NAME"),
    )

@app.route("/")
def home():
    return "<h1>Welcome to E-Commerce and Inventory Management System!</h1><a href='/products'>View Products</a>"

@app.route("/products")
def products():
    conn = get_connection()
    cursor = conn.cursor(dictionary=True)
    cursor.execute(
        "SELECT id, name, price, stock_quantity, low_stock_threshold FROM products"
    )
    items = cursor.fetchall()
    cursor.close()
    conn.close()
    return render_template("products.html", products=items)

if __name__ == "__main__":
    app.run(debug=True)