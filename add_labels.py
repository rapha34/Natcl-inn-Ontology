#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import re

# Read the file
with open('instanciation_P-3178530410105_notags.drawio', 'r', encoding='utf-8') as f:
    content = f.read()

# Pattern to find relations without labels
pattern = r'(        <mxCell id="r-prod-tag(\d+)" edge="1" parent="1" source="prod" style="endArrow=classic;html=1;strokeColor=#A8201A;fontColor=#143642;endSize=8;rounded=0;" target="tag\2">\n          <mxGeometry relative="1" as="geometry" />\n        </mxCell>)'

def replacement(match):
    full_match = match.group(1)
    tag_num = match.group(2)
    
    # Check if this already has a label (lbl) - if yes, skip
    if f'id="lbl{tag_num}"' in content:
        return full_match
    
    # Add the label
    label_xml = f'''        <mxCell id="r-prod-tag{tag_num}" edge="1" parent="1" source="prod" style="endArrow=classic;html=1;strokeColor=#A8201A;fontColor=#143642;endSize=8;rounded=0;" target="tag{tag_num}">
          <mxGeometry relative="1" as="geometry" />
          <mxCell id="lbl{tag_num}" connectable="0" parent="r-prod-tag{tag_num}" style="text;html=1;align=center;verticalAlign=middle;resizable=0;points=[];labelBackgroundColor=#ffffff;fontColor=#143642;fontSize=9;fontStyle=1" value="ncl:hasTagCheck" vertex="1">
            <mxGeometry relative="1" x="0" as="geometry">
              <mxPoint y="-10" as="offset" />
            </mxGeometry>
          </mxCell>
        </mxCell>'''
    return label_xml

content = re.sub(pattern, replacement, content)

# Write back
with open('instanciation_P-3178530410105_notags.drawio', 'w', encoding='utf-8') as f:
    f.write(content)

print('Labels added to all remaining relations')
