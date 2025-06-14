from http.client import HTTPResponse

from flask import Blueprint, request, Response, jsonify, make_response, redirect
import json

from urllib3 import BaseHTTPResponse

from app.backend_client import get_client

auth = Blueprint("auth",__name__)


@auth.route("/login", methods=['POST'])
def login():
    data = request.get_json()
    client = get_client()
    response = client.request("POST","/auth/login",data=data)

    return forward_token(response)


@auth.route("/register", methods=['POST'])
def register():
    data = request.get_json()
    client = get_client()
    response = client.request("POST", "/auth/register", data=data)

    return forward_token(response)

@auth.route("/logout", methods=["POST"])
def logout():
    token = request.cookies.get('JWT')
    refresh = request.cookies.get('RefreshToken')
    if not refresh:
        return jsonify({"message":"Already logged out"}), 200

    client = get_client()

    client.request("POST", "/auth/logout")

    response = make_response(redirect("/"))

    # Clear the cookies (set Max-Age=0)
    response.set_cookie("JWT", "", max_age=0, path="/", httponly=True, secure=True, samesite='Lax')
    response.set_cookie("RefreshToken", "", max_age=0, path="/", httponly=True, secure=True, samesite='Lax')
    return response


def forward_token(response: BaseHTTPResponse):
    resp_body_bytes = bytes(response.data)
    resp_body_str = resp_body_bytes.decode('utf-8')

    resp_body_dict = dict()
    try:
        # Parse JSON body into a Python dict
        resp_body_dict = json.loads(resp_body_str)
        resp_body_dict["info"].pop("accessToken", None)
        resp_body_dict["info"].pop("refreshToken", None)
    except json.JSONDecodeError:
        # Fall back if it's not JSON
        resp_body_dict["info"] = {"message": "Something went wrong"}

    flask_response = Response(
        response=json.dumps(resp_body_dict),
        status=response.status,
        mimetype='application/json'
    )

    set_cookies = response.headers.getlist("Set-Cookie")
    for cookie in set_cookies:
        flask_response.headers.add("Set-Cookie", cookie)

    return flask_response

