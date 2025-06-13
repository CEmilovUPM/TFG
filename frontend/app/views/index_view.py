from flask import Blueprint, render_template, session, redirect, request

from app.backend_client import get_client

main = Blueprint('main', __name__)


@main.route("/")
def login_form():
    token = request.cookies.get('JWT')
    if token:
        client = get_client()

        headers = {'Authorization': f'Bearer {token}'}
        resp = client.request("get", "user/profile", headers=headers)

        if resp.status == 200:
            redirect('/home')


    return render_template("index.html")




#deprecated
def home():
    token = request.cookies.get('JWT')
    if not token:
        return redirect('/')

    client = get_client()

    headers = {'Authorization': f'Bearer {token}'}
    resp = client.request("get","user/profile",headers=headers)

    if resp.status == 401 or resp.status == 403:


        return redirect('/')


    return render_template("base.html")