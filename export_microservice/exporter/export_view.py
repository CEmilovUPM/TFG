import csv
import io
import json

from flask import Response, request, redirect, jsonify, Blueprint, send_file
from reportlab.lib.pagesizes import letter
from reportlab.pdfgen import canvas

from exporter.backend_client import Token, RequestBuilder, get_client

export = Blueprint("export",__name__)

def profile(token: Token, refresh: Token):
    client = get_client()
    backend_request = RequestBuilder()
    backend_request.auth(token).refresh(refresh).set_method("get").set_endpoint("/user/profile")
    response = client.request_reauth(backend_request)

    if response.status in [401, 403]:
        return jsonify({'error': 'unauthorized'}), 401

    body = json.loads(response.data)
    return body, 200

def get_progress_by_goal_id(user_id,goal_id, token:Token, refresh:Token):
    client = get_client()
    backend_request = (
        RequestBuilder()
        .auth(token)
        .refresh(refresh)
        .set_method("get")
        .set_endpoint(f"/user/{user_id}/goals/{goal_id}/progress")
    )
    response = client.request_reauth(backend_request)
    return json.loads(response.data)["data"]



@export.route('/user/<int:user_id>/csv-report')
def export_csv(user_id):
    token = Token(request.cookies.get('JWT'))
    refresh = Token(request.cookies.get('RefreshToken'))
    if not refresh.value:
        return redirect('/')

    client = get_client()
    backend_request = (
        RequestBuilder()
        .auth(token)
        .refresh(refresh)
        .set_method("get")
        .set_endpoint(f"/user/{user_id}/goals")
    )
    response = client.request_reauth(backend_request)

    if response.status in [401, 403]:
        return redirect('/')

    goals = json.loads(response.data)["data"]

    output = io.StringIO()
    writer = csv.DictWriter(output, fieldnames=[
        'goal_id', 'goal_title', 'goal_description', 'goal_metric', 'goal_totalDesiredAmount', 'goal_creationDate',
        'goal_isCompleted',
        'progress_id', 'progress_note', 'progress_amount', 'progress_date'
    ])
    writer.writeheader()

    for goal in goals:
        progress_list = get_progress_by_goal_id(user_id, goal["id"],token, refresh)
        for progress in progress_list:
            writer.writerow({
                'goal_id': goal['id'],
                'goal_title': goal['title'],
                'goal_description': goal['description'],
                'goal_metric': goal['metric'],
                'goal_totalDesiredAmount': goal['totalDesiredAmount'],
                'goal_creationDate': goal['creationDate'],
                'goal_isCompleted': goal['completed'],
                'progress_id': progress['id'],
                'progress_note': progress['updateNote'],
                'progress_amount': progress['amount'],
                'progress_date': progress['date'],
            })

    response = Response(output.getvalue(), mimetype='text/csv')
    response.headers["Content-Disposition"] = "attachment; filename=data-report.csv"
    response.headers["JWT"] = token.value
    return response

@export.route('/user/<int:user_id>/pdf-report')
def export_pdf(user_id):
    token = Token(request.cookies.get('JWT'))
    refresh = Token(request.cookies.get('RefreshToken'))
    if not refresh.value:
        return redirect('/')

    client = get_client()

    goals_request = (
        RequestBuilder()
        .auth(token)
        .refresh(refresh)
        .set_method("get")
        .set_endpoint(f"/user/{user_id}/goals")
    )
    goals_response = client.request_reauth(goals_request)
    if goals_response.status in [401, 403]:
        return redirect('/')
    goals = json.loads(goals_response.data)["data"]

    user_data, status = profile(token, refresh)
    if status in [401, 403]:
        return redirect('/')
    user = user_data["data"][0]

    buffer = io.BytesIO()
    pdf = canvas.Canvas(buffer, pagesize=letter)
    width, height = letter

    draw_pdf_header(pdf, user, width, height)
    y = height - 70
    line_height = 16

    for goal in goals:
        y = draw_goal_section(pdf, user_id, goal, token, refresh, y, line_height, width, height)

    pdf.save()
    buffer.seek(0)

    return send_file(
        buffer,
        as_attachment=True,
        download_name="goal-progress-report.pdf",
        mimetype='application/pdf'
    )


def draw_pdf_header(pdf, user, width, height):
    pdf.setTitle("Goal Progress Report")
    pdf.setFont("Helvetica-Bold", 16)
    pdf.drawString(
        30, height - 40,
        f"Goal Progress Report for User {user['name']}, {user['email']} (ID={user['id']})"
    )


def draw_goal_section(pdf, user_id, goal, token, refresh, y, line_height, width, height):
    if y < 70:
        pdf.showPage()
        y = height - 50

    # Goal header
    pdf.setFont("Helvetica-Bold", 12)
    pdf.drawString(30, y, f"Goal: {goal['title']} (ID: {goal['id']})")
    y -= line_height

    # Goal metadata
    pdf.setFont("Helvetica-Oblique", 9)
    pdf.drawString(40, y, f"Description: {goal.get('description', '')}")
    y -= line_height
    pdf.drawString(
        40, y,
        f"Metric: {goal.get('metric', '')} | Total Desired: {goal.get('totalDesiredAmount', '')} | "
        f"Created: {goal.get('creationDate', '')} | Completed: {goal.get('completed', False)}"
    )
    y -= line_height

    # Progress entries
    pdf.setFont("Helvetica", 10)
    progress_list = get_progress_by_goal_id(user_id, goal["id"], token, refresh)
    if not progress_list:
        pdf.drawString(50, y, "No progress recorded.")
        y -= line_height
    else:
        for progress in progress_list:
            if y < 50:
                pdf.showPage()
                y = height - 50
                pdf.setFont("Helvetica", 10)

            pdf.drawString(
                50, y,
                f"- [{progress['date']}] {progress['updateNote']} (Amount: {progress['amount']})"
            )
            y -= line_height

    return y - line_height