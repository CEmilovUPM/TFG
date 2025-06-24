from flask import Blueprint

protected = Blueprint("protected", __name__)

from . import protected_view