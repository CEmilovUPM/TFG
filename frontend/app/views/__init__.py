from flask import Blueprint

protected = Blueprint("protected", __name__)

from . import protected_view
from . import goals_view
from . import progress_view