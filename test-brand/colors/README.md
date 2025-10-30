# Healthcare Test Brand Color Palette

Healthcare-inspired brand colors designed for trust, professionalism, and patient care. **Note**: All teal/cyan colors have been remapped to magenta variants to ensure solid color rendering without gradients.

## Primary Brand Colors

| Color | Hex Code | RGB | Usage |
|-------|----------|-----|-------|
| **Healthcare Magenta** | `#A6216B` | 166, 33, 107 | Primary brand color, navigation, headings |
| **Healthcare Dark Magenta** | `#7A1B4A` | 122, 27, 74 | Secondary brand, dark variants |
| **Healthcare Blue** | `#003366` | 0, 51, 102 | Supporting blue for depth |
| **Healthcare Navy** | `#002244` | 0, 34, 68 | Deep accent, professional tone |

## Color Remapping for Gradient Elimination

To ensure solid colors without browser gradient rendering issues:

| Original Variable | Remapped Color | Purpose |
|-------------------|----------------|---------|
| `--healthcare-teal` | `#A6216B` (magenta) | Eliminated teal gradients |
| `--healthcare-cyan` | `#A6216B` (magenta) | Eliminated cyan gradients |
| `--healthcare-light-cyan` | `#B83C7A` (light magenta) | Light variant |
| `--healthcare-dark-cyan` | `#7A1B4B` (dark magenta) | Dark variant |

## Generic Brand Variable Mapping

| Generic Variable | Maps To | Color |
|------------------|---------|-------|
| `--brand-primary` | `--healthcare-magenta` | `#A6216B` |
| `--brand-secondary` | `--healthcare-dark-magenta` | `#7A1B4A` |
| `--brand-accent` | `--healthcare-magenta` | `#A6216B` |
| `--brand-focus` | `--healthcare-magenta` | `#A6216B` |
| **Medium Blue** | `#2C5F88` | 44, 95, 136 | Mid-tone applications |
| **Dark Blue** | `#1A3A55` | 26, 58, 85 | Deep backgrounds, contrast |
| **Light Magenta** | `#C564A3` | 197, 100, 163 | Hover states, light accents |
| **Dark Magenta** | `#7A1B4A` | 122, 27, 74 | Deep magenta accents |
| **Light Cyan** | `#33C2D9` | 51, 194, 217 | Light cyan accents, highlights |
| **Dark Cyan** | `#007A8C` | 0, 122, 140 | Deep cyan applications |

## Neutral Colors

Foundation colors for layouts and typography, using cleaner grays.

| Color | Hex Code | RGB | Usage |
|-------|----------|-----|-------|
| **White** | `#FFFFFF` | 255, 255, 255 | Backgrounds, clean space |
| **Light Gray** | `#F5F5F5` | 245, 245, 245 | Subtle backgrounds |
| **Medium Gray** | `#CCCCCC` | 204, 204, 204 | Borders, dividers |
| **Dark Gray** | `#666666` | 102, 102, 102 | Secondary text |
| **Charcoal** | `#333333` | 51, 51, 51 | Primary text |
| **Black** | `#000000` | 0, 0, 0 | High contrast text |

## Semantic Colors

Status and feedback colors following healthcare standards.

| Color | Hex Code | RGB | Usage |
|-------|----------|-----|-------|
| **Success** | `#28A745` | 40, 167, 69 | Success states, positive outcomes |
| **Warning** | `#FFC107` | 255, 193, 7 | Caution, attention needed |
| **Error** | `#DC3545` | 220, 53, 69 | Errors, critical alerts |
| **Info** | `#17A2B8` | 23, 162, 184 | Information, neutral alerts |

## Healthcare Color Psychology

Colors chosen to evoke:
- **Trust and Reliability**: Deep blues convey medical expertise
- **Calm and Healing**: Teal tones promote wellness and peace
- **Professional Excellence**: Navy establishes healthcare authority
- **Accessibility**: High contrast ratios for all users

## Accessibility Compliance

All color combinations meet WCAG 2.1 AA standards:
- Normal text: minimum 4.5:1 contrast ratio
- Large text (18pt+ or 14pt+ bold): minimum 3:1 contrast ratio
- Interactive elements: clear focus indicators

## Usage Guidelines

### Primary Applications
- **Healthcare Blue**: Main branding, headers, primary CTAs
- **Healthcare Teal**: Secondary branding, wellness content
- **Healthcare Navy**: Professional content, footer areas

### Do's
- Use primary colors for main brand elements
- Maintain sufficient contrast for accessibility
- Apply teal for calming, healing-focused content
- Use navy for professional, authoritative sections

### Don'ts
- Don't use pure black or white for text (use charcoal/off-white)
- Don't combine low-contrast color pairs
- Don't use bright colors for large areas
- Don't override semantic color meanings

## Implementation

### CSS Variables
```css
--healthcare-blue: #1B365D;
--healthcare-teal: #4A9B8E;
--healthcare-navy: #0F2A44;
```

### Gradients
- **Primary**: Blue to Teal gradient for hero sections
- **Secondary**: Navy to Blue for professional areas
- **Accent**: Teal variations for wellness content