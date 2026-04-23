import re

with open("src/services/PharmacyService.java", "r") as f:
    content = f.read()

# We will replace the entire file since there are many changes.
