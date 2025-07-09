import re
from datetime import datetime, date, timedelta
import json
from copy import deepcopy
import calendar

from flask import Blueprint, request, redirect, render_template, jsonify, send_from_directory, make_response
import plotly.graph_objs as go

from graph.backend_client import get_client, RequestBuilder, Token
from graph.config import STATIC_URL

protected = Blueprint("protected", __name__)

@protected.route("/user/<int:user_id>/goals/<int:goal_id>/graph", methods=["GET"])
def render_graph(user_id, goal_id):
    token = Token(request.cookies.get('JWT'))
    refresh = Token(request.cookies.get('RefreshToken'))
    if not refresh.value:
        return redirect('/')

    user_data, _ = profile(token, refresh)
    user_data = user_data["data"][0]

    client = get_client()

    backend_request = (
        RequestBuilder()
        .auth(token)
        .refresh(refresh)
        .set_method("get")
        .set_endpoint(f"/user/{user_id}/goals/{goal_id}/progress")
    )
    response = client.request_reauth(backend_request)

    if response.status in [401, 403]:
        return redirect('/')

    progress_list = json.loads(response.data)["data"]
    progress_list.sort(key=lambda x: x["date"])

    selected_month = request.args.get("month")
    if selected_month is None:
        selected_month = datetime.now().strftime("%Y-%m")

    filtered_progress = [p for p in progress_list if p['date'].startswith(selected_month)]

    year, month = map(int, selected_month.split("-"))
    num_days = calendar.monthrange(year, month)[1]
    all_dates = [date(year, month, day) for day in range(1, num_days + 1)]

    if not filtered_progress:
        if request.args.get("partial") == "true":
            fig = go.Figure()
            fig.update_layout(
                xaxis=dict(showgrid=False, zeroline=False, visible=False),
                yaxis=dict(showgrid=False, zeroline=False, visible=False),
                annotations=[dict(
                    text="No data for the month picked",
                    xref="paper", yref="paper",
                    showarrow=False,
                    font=dict(size=20)
                )]
            )
            graph_json = json.loads(fig.to_json())
            return jsonify(graph_json)
        else:
            return render_template(
                "graph_template.html",
                graph_html="",
                **user_data,
                user_id=user_id,
                goal_id=goal_id,
                STATIC_URL=STATIC_URL
            )

    date_amount_map = {}
    for p in filtered_progress:
        d = datetime.strptime(p["date"], "%Y-%m-%d").date()
        date_amount_map[d] = date_amount_map.get(d, 0) + p["amount"]

    amounts = [date_amount_map.get(d, 0) for d in all_dates]

    fig = go.Figure()
    fig.add_trace(go.Bar(
        x=all_dates,
        y=amounts,
        name='Progress',
        hovertemplate='Date: %{x|%Y-%m-%d}<br>Amount: %{y}<extra></extra>'
    ))

    fig.update_xaxes(
        type='date',
        tickformat="%b %d",
        dtick=86400000,
        range=[all_dates[0], all_dates[-1]]
    )

    max_amount = max(amounts) if amounts else 1
    fig.update_yaxes(range=[0, max_amount * 1.1])

    if request.args.get("partial") == "true":
        graph_json = json.loads(fig.to_json())
        return jsonify(graph_json)

    graph_html = ""

    resp = make_response(render_template(
        "graph_template.html",
        graph_html=graph_html,
        **user_data,
        user_id=user_id,
        goal_id=goal_id,
        STATIC_URL=STATIC_URL
    ))
    resp.set_cookie("JWT", token.value)
    return resp


def profile(token: Token, refresh: Token):
    client = get_client()
    backend_request = RequestBuilder()
    backend_request.auth(token).refresh(refresh).set_method("get").set_endpoint("/user/profile")
    response = client.request_reauth(backend_request)

    if response.status in [401, 403]:
        return jsonify({'error': 'unauthorized'}), 401

    body = json.loads(response.data)
    return body, 200
