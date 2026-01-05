import pandas as pd
from pandas import ExcelWriter
from pathlib import Path

SRC = Path(r"C:\Users\conde-salazar\Documents\GitHub\Natcl-inn-Ontology\Lot 1.1 Attributs Naturalité Conso LEGO NATCLINN V3.0_dedouble.xlsx")
SHEET = "Arguments reformulés"


def apply_rule(df: pd.DataFrame, mask: pd.Series, pos_update: dict, neg_update: dict, name_property: str | None, changes: list):
    for idx, row in df[mask].iterrows():
        before = row.copy()
        pol = "" if pd.isna(row.get("Polarité")) else str(row.get("Polarité")).strip()
        if pol not in {"+", "-"}:
            continue
        if pol == "+":
            for k, v in pos_update.items():
                df.at[idx, k] = v
            if name_property:
                df.at[idx, "NameProperty"] = name_property
        else:
            for k, v in neg_update.items():
                df.at[idx, k] = v
            if name_property:
                df.at[idx, "NameProperty"] = name_property
        # Log des changements clés
        for field in ("Assertion", "Aim", "NameProperty", "Value"):
            changes.append({
                "Row": idx,
                "Attributs ": before.get("Attributs "),
                "Polarité": pol,
                "Field": field,
                "Before": before.get(field),
                "After": df.at[idx, field]
            })


def adjust_column_widths(ws, padding: int = 2, max_width: int = 80):
    """Ajuste la largeur des colonnes en fonction du contenu."""
    for col_cells in ws.columns:
        max_len = 0
        col_letter = col_cells[0].column_letter if col_cells else None
        for cell in col_cells:
            if cell.value is None:
                continue
            value_len = len(str(cell.value))
            if value_len > max_len:
                max_len = value_len
        if col_letter:
            target_width = min(max_len + padding, max_width)
            ws.column_dimensions[col_letter].width = target_width


def main():
    if not SRC.exists():
        raise FileNotFoundError(f"Fichier introuvable: {SRC}")

    df = pd.read_excel(SRC, sheet_name=0)
    # Assure colonnes attendues
    required_cols = [
        "Attributs ", "Description", "Verbatim représentatif",
        "Assertion", "Polarité", "NameCriterion", "Aim",
        "NameProperty", "Value", "Condition", "InfValue", "SupValue", "Unit"
    ]
    for col in required_cols:
        if col not in df.columns:
            df[col] = pd.NA

    sel = df["Attributs "].fillna("")
    changes: list[dict] = []

    # Bien-être animal (ajusté précédemment, on réapplique pour homogénéité)
    mask = sel.str.contains("Bien-être animal", case=False, regex=False)
    apply_rule(
        df,
        mask,
        pos_update={
            "Assertion": "Respecter le bien-être animal renforce la naturalité perçue.",
            "Aim": "Respecter le bien-être animal pour renforcer la naturalité perçue.",
            "Value": "Respect du bien-être animal",
        },
        neg_update={
            "Assertion": "La souffrance animale perçue réduit la naturalité perçue.",
            "Aim": "Éviter la souffrance animale perçue pour préserver la naturalité.",
            "Value": "Souffrance animale perçue",
        },
        name_property="Bien-être animal",
        changes=changes,
    )

    # Intrants chimiques
    mask = sel.str.contains("sans intrants", case=False, regex=False)
    apply_rule(
        df,
        mask,
        pos_update={
            "Assertion": "L’absence d’intrants chimiques renforce la naturalité perçue.",
            "Aim": "Éviter l’usage d’intrants chimiques de synthèse.",
            "Value": "Absence d’intrants chimiques",
        },
        neg_update={
            "Assertion": "L’usage d’intrants chimiques réduit la naturalité perçue.",
            "Aim": "Limiter l’usage d’intrants chimiques de synthèse.",
            "Value": "Présence d’intrants chimiques",
        },
        name_property="Intrants chimiques",
        changes=changes,
    )

    # Production par particulier
    mask = sel.str.contains("particulier", case=False, regex=False)
    apply_rule(
        df,
        mask,
        pos_update={
            "Assertion": "La production par un particulier renforce la naturalité perçue.",
            "Aim": "Favoriser la production par des particuliers.",
            "Value": "Production par particulier",
        },
        neg_update={
            "Assertion": "Une production non réalisée par des particuliers réduit la naturalité perçue.",
            "Aim": "Éviter les modes de production perçus comme impersonnels.",
            "Value": "Production non particulière",
        },
        name_property="Mode de production",
        changes=changes,
    )

    # Production locale
    mask = sel.str.contains("locale", case=False, regex=False)
    apply_rule(
        df,
        mask,
        pos_update={
            "Assertion": "La production locale renforce la naturalité perçue.",
            "Aim": "Privilégier les productions locales.",
            "Value": "Locale",
        },
        neg_update={
            "Assertion": "L’éloignement géographique réduit la naturalité perçue.",
            "Aim": "Éviter les chaînes d’approvisionnement éloignées.",
            "Value": "Non locale",
        },
        name_property="Localité de production",
        changes=changes,
    )

    # Arômes naturels / artificiels
    mask = sel.str.contains("arôme", case=False, regex=False)
    apply_rule(
        df,
        mask,
        pos_update={
            "Assertion": "L’utilisation d’arômes d’origine naturelle renforce la naturalité perçue.",
            "Aim": "Garantir l’utilisation d’arômes d’origine naturelle.",
            "Value": "Arômes naturels",
        },
        neg_update={
            "Assertion": "L’utilisation d’arômes artificiels réduit la naturalité perçue.",
            "Aim": "Éviter les arômes artificiels.",
            "Value": "Arômes artificiels",
        },
        name_property="Type d’arômes",
        changes=changes,
    )

    # Additifs
    mask = sel.str.contains("additif", case=False, regex=False)
    apply_rule(
        df,
        mask,
        pos_update={
            "Assertion": "L’absence d’additifs perçus comme artificiels renforce la naturalité perçue.",
            "Aim": "Limiter l’usage d’additifs non naturels.",
            "Value": "Sans additifs artificiels",
        },
        neg_update={
            "Assertion": "La présence d’additifs artificiels réduit la naturalité perçue.",
            "Aim": "Éviter les additifs artificiels.",
            "Value": "Additifs artificiels",
        },
        name_property="Additifs",
        changes=changes,
    )

    # Transformation / Processus
    mask = sel.str.contains("Transformation|process", case=False, regex=True)
    apply_rule(
        df,
        mask,
        pos_update={
            "Assertion": "Une transformation minimale renforce la naturalité perçue.",
            "Aim": "Favoriser des procédés perçus comme peu transformants.",
            "Value": "Transformation minimale",
        },
        neg_update={
            "Assertion": "Une transformation jugée intensive réduit la naturalité perçue.",
            "Aim": "Éviter des procédés perçus comme intensifs.",
            "Value": "Transformation intensive",
        },
        name_property="Niveau de transformation",
        changes=changes,
    )

    # Emballage
    mask = sel.str.contains("emball", case=False, regex=True)
    apply_rule(
        df,
        mask,
        pos_update={
            "Assertion": "Un emballage perçu comme simple et respectueux renforce la naturalité perçue.",
            "Aim": "Privilégier des emballages simples et recyclables.",
            "Value": "Emballage simple/recyclable",
        },
        neg_update={
            "Assertion": "Un emballage perçu comme excessif réduit la naturalité perçue.",
            "Aim": "Éviter les emballages perçus comme excessifs.",
            "Value": "Emballage perçu comme excessif",
        },
        name_property="Type d’emballage",
        changes=changes,
    )

    # Origine contrôlée / Label
    mask = sel.str.contains("certifi|label", case=False, regex=True)
    apply_rule(
        df,
        mask,
        pos_update={
            "Assertion": "Une origine certifiée renforce la naturalité perçue.",
            "Aim": "Privilégier des labels perçus comme gages de naturalité.",
            "Value": "Origine/label certifié",
        },
        neg_update={
            "Assertion": "L’absence de certification réduit la naturalité perçue.",
            "Aim": "Éviter l’absence de labels reconnus.",
            "Value": "Origine/label non certifié",
        },
        name_property="Certification/label",
        changes=changes,
    )

    # Écriture avec feuille "Changements"
    changes_df = pd.DataFrame(changes, columns=["Row", "Attributs ", "Polarité", "Field", "Before", "After"])  # type: ignore[arg-type]
    with ExcelWriter(SRC, engine="openpyxl", mode="w") as w:
        df.to_excel(w, sheet_name=SHEET, index=False)
        changes_df.to_excel(w, sheet_name="Changements", index=False)

        # Ajuste les largeurs de colonnes sur les deux feuilles
        for ws in w.sheets.values():
            adjust_column_widths(ws)

    print(f"Sauvegardé: {SRC}")
    print(f"Nombre de changements consignés: {len(changes_df)}")


if __name__ == "__main__":
    main()
