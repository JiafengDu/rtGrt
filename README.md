# rtGrt

### Setting Up the Project (First Time / New Machine)

1.  **Clone the Repository:**
    ```bash
    git clone <your-repo-url>
    cd rtGrt
    ```

2.  **Create a Virtual Environment:**
    It's recommended to create a virtual environment to isolate project dependencies.
    ```bash
    python -m venv venv
    # Or use python3 if python points to Python 2
    # python3 -m venv venv
    ```

3.  **Activate the Virtual Environment:**
    *   **Linux / macOS / WSL:**
        ```bash
        source venv/bin/activate
        ```
    *   **Windows (Command Prompt):**
        ```bash
        .\venv\Scripts\activate.bat
        ```
    *   **Windows (PowerShell or Git Bash):**
        ```bash
        .\venv\Scripts\activate
        ```
    Your terminal prompt should now be prefixed with `(venv)`.

4.  **Install Dependencies:**
    Install all the packages listed in `requirements.txt`.
    ```bash
    pip install -r requirements.txt
    ```

Your environment is now set up with the correct dependencies to run the application.

### Adding or Updating Dependencies

If you need to add a new package or update an existing one during development:

1.  **Activate the Virtual Environment** (if not already active):
    ```bash
    source venv/bin/activate
    # Or use the appropriate Windows command
    ```

2.  **Install or Update the Package:**
    ```bash
    # Example: Install a new package
    pip install new-package-name

    # Example: Upgrade an existing package
    pip install --upgrade flask
    ```

3.  **Update `requirements.txt`:**
    **Crucially**, regenerate the `requirements.txt` file to reflect the changes in your environment.
    ```bash
    pip freeze > requirements.txt
    ```

4.  **Commit the Changes:**
    Add the updated `requirements.txt` file to your Git commit so others will get the new dependency list.
    ```bash
    git add requirements.txt
    git commit -m "Update dependencies (added/updated package-name)"
    ```
