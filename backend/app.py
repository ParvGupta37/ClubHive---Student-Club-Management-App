# app.py
import os
from flask import Flask, request, jsonify
import sqlite3
from flask_cors import CORS
from dotenv import load_dotenv
from werkzeug.security import generate_password_hash, check_password_hash
from flask_jwt_extended import (
    JWTManager, create_access_token, jwt_required, get_jwt_identity, verify_jwt_in_request_optional
)
from typing import Optional

# Load .env if present
load_dotenv()

app = Flask(__name__)
# Allow all origins for development. In production set origins explicitly.
CORS(app, resources={r"/api/*": {"origins": "*"}})

# Configuration
DB_PATH = os.getenv("DB_PATH") or os.path.join(os.path.dirname(__file__), "unihub.db")
app.config["JWT_SECRET_KEY"] = os.getenv("JWT_SECRET_KEY", "replace-this-with-a-secret")
app.config["PROPAGATE_EXCEPTIONS"] = True

jwt = JWTManager(app)

# ---------- Helper: Database connection ----------
def get_db_connection():
    # check_same_thread=False helps when Flask dev server uses threads.
    conn = sqlite3.connect(DB_PATH, check_same_thread=False)
    conn.row_factory = sqlite3.Row
    return conn

def query_db(query: str, args=(), one: bool=False):
    conn = get_db_connection()
    try:
        cur = conn.execute(query, args)
        rows = cur.fetchall()
        return (rows[0] if rows else None) if one else rows
    finally:
        conn.close()

def dictify_rows(rows):
    return [dict(r) for r in rows]

def json_error(message: str, status: int = 400):
    return jsonify({"success": False, "message": message}), status

# ---------- Utility: get optional authenticated club id ----------
def get_requesting_club_id() -> Optional[int]:
    """
    If a valid JWT exists in the request, return the club_id (identity).
    If none, return None.
    """
    try:
        # Will not raise if no token present, because optional
        verify_jwt_in_request_optional()
        identity = get_jwt_identity()
        return int(identity) if identity is not None else None
    except Exception:
        return None

# ---------- LOGIN ----------
@app.route('/api/login', methods=['POST'])
def login():
    """
    Expects JSON: { "email": "...", "password": "..." }
    Returns: { success: True, access_token: "...", club_id, club_name, email }
    """
    data = request.get_json(silent=True) or {}
    email = data.get('email')
    password = data.get('password')

    if not email or not password:
        return json_error("Missing email or password", 400)

    conn = get_db_connection()
    try:
        user = conn.execute(
            "SELECT * FROM clubs WHERE email = ?",
            (email,)
        ).fetchone()
    finally:
        conn.close()

    if not user:
        return json_error("Invalid credentials", 401)

    stored_hash = user["password_hash"]
    if not stored_hash:
        return json_error("No password set for user", 500)

    if not check_password_hash(stored_hash, password):
        return json_error("Invalid credentials", 401)

    # identity is the club_id so we can filter requests by club later
    access_token = create_access_token(identity=user["club_id"])

    return jsonify({
        "success": True,
        "access_token": access_token,
        "club_id": user["club_id"],
        "club_name": user["club_name"],
        "email": user["email"],
    })


# ---------- DATA FETCH ROUTES ----------
# Behavior:
# - If request has a valid JWT, endpoints will return only records for that club.
# - If no JWT, endpoints support optional ?club_id= query param to fetch public data.

def get_filter_clause_for_club(param_club_id: Optional[str], auth_club_id: Optional[int]):
    """
    Returns tuple (clause, args) for SQL WHERE clause that filters by club.
    If auth_club_id is present, it takes precedence and we filter by that.
    Otherwise, if param_club_id provided, use that.
    If neither, return empty clause.
    """
    if auth_club_id is not None:
        return ("WHERE club_id = ?", (auth_club_id,))
    if param_club_id:
        try:
            cid = int(param_club_id)
            return ("WHERE club_id = ?", (cid,))
        except ValueError:
            pass
    return ("", ())


@app.route("/api/clubs", methods=["GET"])
def get_clubs():
    # Clubs list is generally public; we do not require auth
    rows = query_db("SELECT club_id, club_name, email, description FROM clubs")
    return jsonify(dictify_rows(rows))


@app.route("/api/members", methods=["GET"])
def get_members():
    auth_club_id = get_requesting_club_id()
    param_club_id = request.args.get("club_id")
    clause, args = get_filter_clause_for_club(param_club_id, auth_club_id)
    sql = f"SELECT * FROM members {clause} ORDER BY name"
    rows = query_db(sql, args)
    return jsonify(dictify_rows(rows))


@app.route("/api/events", methods=["GET"])
def get_events():
    auth_club_id = get_requesting_club_id()
    param_club_id = request.args.get("club_id")
    clause, args = get_filter_clause_for_club(param_club_id, auth_club_id)
    sql = f"SELECT * FROM events {clause} ORDER BY event_date DESC"
    rows = query_db(sql, args)
    return jsonify(dictify_rows(rows))


@app.route("/api/meetings", methods=["GET"])
def get_meetings():
    auth_club_id = get_requesting_club_id()
    param_club_id = request.args.get("club_id")
    clause, args = get_filter_clause_for_club(param_club_id, auth_club_id)
    sql = f"SELECT * FROM meetings {clause} ORDER BY meeting_date DESC"
    rows = query_db(sql, args)
    return jsonify(dictify_rows(rows))


@app.route("/api/announcements", methods=["GET"])
def get_announcements():
    auth_club_id = get_requesting_club_id()
    param_club_id = request.args.get("club_id")
    clause, args = get_filter_clause_for_club(param_club_id, auth_club_id)
    sql = f"SELECT * FROM announcements {clause} ORDER BY created_at DESC"
    rows = query_db(sql, args)
    return jsonify(dictify_rows(rows))


@app.route("/api/notifications", methods=["GET"])
def get_notifications():
    auth_club_id = get_requesting_club_id()
    param_club_id = request.args.get("club_id")
    clause, args = get_filter_clause_for_club(param_club_id, auth_club_id)
    sql = f"SELECT * FROM notifications {clause} ORDER BY created_at DESC"
    rows = query_db(sql, args)
    return jsonify(dictify_rows(rows))


# Example protected endpoint that MUST be accessed by authenticated club only
@app.route("/api/profile", methods=["GET"])
@jwt_required()
def get_profile():
    current_club_id = get_jwt_identity()
    row = query_db("SELECT club_id, club_name, email, description FROM clubs WHERE club_id = ?", (current_club_id,), one=True)
    if not row:
        return json_error("Club not found", 404)
    return jsonify(dict(row))


# ---------- Utility endpoints for dev only ----------
@app.route("/api/_hash_password", methods=["POST"])
def hash_password_helper():
    """
    Dev helper: POST { "password": "..." } 
    returns {"hash": "..."} to allow you to insert into DB.
    REMOVE or protect this endpoint in production.
    """
    data = request.get_json(silent=True) or {}
    password = data.get("password")
    if not password:
        return json_error("Missing password", 400)
    hashed = generate_password_hash(password)
    return jsonify({"hash": hashed})


# Generic error handler for JSON responses
@app.errorhandler(Exception)
def handle_exception(e):
    # In production you might want to log the stack trace to file instead
    message = str(e)
    # Do not leak complex exception details in production
    return jsonify({"success": False, "message": message}), 500


if __name__ == "__main__":
    # Quick sanity: create DB path if does not exist (not creating tables)
    if not os.path.exists(DB_PATH):
        app.logger.warning(f"Database file not found at {DB_PATH}. Make sure it exists.")
    app.run(host="127.0.0.1", port=5000, debug=True)
