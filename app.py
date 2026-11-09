from flask import Flask

app = Flask(__name__)

@app.route("/")
def home():
    return "<h1>Welcome to E-Commerce and Inventory Management System!</h1>"

if __name__ == "__main__":
    app.run(debug=True)