import re
from datetime import datetime

from flask import json, make_response

from app.backend_client import Token, get_client, RequestBuilder


# pass request.args
def filter_goals_list(request_args, goals):
    q = request_args.get('q', '').lower()
    filtered_goals = goals
    if q:
        pattern = re.compile(re.escape(q), re.IGNORECASE)
        filtered_goals = [g for g in goals if pattern.search(g['title'])]

    return filtered_goals

def filter_single_goal(request_args, progress_list):
    date = request_args.get('date', '').lower()
    filtered_progress = progress_list

    if date:
        filtered_progress  = [p for p in progress_list if p["date"] == date]

    q = request_args.get('q', '').lower()
    if q:
        pattern = re.compile(re.escape(q), re.IGNORECASE)
        filtered_progress = [p for p in filtered_progress if pattern.search(p['updateNote'])]

    filtered_progress = sorted(filtered_progress, key=lambda p: p["date"], reverse=True)

    return filtered_progress

def render_date(date: str):
    return datetime.strptime(date, "%Y-%m-%d").strftime("%d/%m/%Y")


def trim_float(value):
    try:
        f = float(value)
        return int(f) if f.is_integer() else round(f,2)
    except (ValueError, TypeError):
        return value


def create_response(content, access_token: Token):
    out = make_response(content)
    out.set_cookie("JWT",access_token.value)
    return out
