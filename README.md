# Natcl-inn-Ontology

## Deployment & quick test

This project includes a VS Code task and a PowerShell script to build and deploy the WAR to your local Tomcat.

- To deploy from VS Code: open the *Run Task* menu and run the task `Build & Deploy to Tomcat` (it calls `scripts/deploy-windows.ps1`).
- To run the script directly from PowerShell:

```powershell
# from the repository root
.
scripts\deploy-windows.ps1 -SkipTests
```

- Quick endpoint test (see the service at `api_natclinn/products/list`):

Using curl (from Windows `cmd` with curl available):

```bash
curl -i http://localhost:8080/NatclinnWebService/api_natclinn/products/list
```

Using PowerShell `Invoke-WebRequest`:

```powershell
Invoke-WebRequest -Uri "http://localhost:8080/NatclinnWebService/api_natclinn/products/list" -UseBasicParsing
```

Look for the `Content-Type: application/json` response header and a JSON array of product objects.

