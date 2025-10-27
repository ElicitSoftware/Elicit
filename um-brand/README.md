# University of Michigan Brand Implementation

Official University of Michigan brand implementation for Michigan Medicine, based on guidelines from [brand.umich.edu](https://brand.umich.edu/).

## Directory Structure

```
um-brand/
├── brand-config.json          # Brand configuration and metadata
├── colors/
│   ├── brand-colors.css          # CSS custom properties for colors
│   └── README.md                 # Color usage guidelines
├── typography/                   # UM typography system
│   ├── brand-typography.css      # CSS typography definitions
│   └── README.md                 # Typography guidelines
├── logos/                     # Official logos and marks (placeholders)
│   ├── um-primary-logo.svg   # Primary University logo
│   ├── um-horizontal-logo.svg # Horizontal layout logo
│   ├── um-block-m.svg        # Block M secondary mark
│   ├── michigan-medicine-logo.svg # Michigan Medicine logo
│   └── README.md             # Logo usage guidelines
├── visual-assets/             # Additional visual elements
│   └── icons/                # Icons and favicon
│       ├── favicon.svg       # University favicon
│       └── README.md         # Icon guidelines
└── README.md                 # This file
```

## Quick Start

### 1. Include CSS Files

```html
<!-- Include UM brand colors -->
<link rel="stylesheet" href="/um-brand/colors/brand-colors.css">

<!-- Include UM typography -->
<link rel="stylesheet" href="/um-brand/typography/brand-typography.css">
```

### 2. Use CSS Variables

```css
/* Primary brand colors */
background-color: var(--um-maize);
color: var(--um-blue);

/* Typography */
font-family: var(--um-font-primary);
font-size: var(--um-font-size-lg);
```

### 3. Apply Typography Classes

```html
<h1 class="um-heading-1">Primary Heading</h1>
<p class="um-body">Body text content</p>
<small class="um-caption">Caption text</small>
```

## Brand Elements

### Colors

**Primary Palette:**
- **Maize**: `#FFCB05` - Primary brand color
- **Blue**: `#00274C` - Secondary brand color, text

**Secondary Palette:**
- Comprehensive palette of supporting colors
- All colors meet WCAG 2.0 AA accessibility standards
- Detailed specifications in `colors/README.md`

### Typography

**Recommended Fonts:**
- **IBM Plex Sans**: Primary sans-serif
- **Montserrat**: Headers and emphasis
- **Nunito Sans**: Body text
- **Merriweather**: Serif applications
- **Atkinson Hyperlegible**: Accessibility-focused

### Logos

**Available Marks:**
- Primary University logo
- Horizontal logo variant
- Block M secondary mark
- Michigan Medicine specific logo

**Important**: Logo files are placeholders. Obtain official logos from:
- [brand.umich.edu/logos/](https://brand.umich.edu/logos/)
- [branding.med.umich.edu/](https://branding.med.umich.edu/)

## Usage Guidelines

### Brand Compliance

1. **Colors**: Use only approved brand colors with proper contrast ratios
2. **Typography**: Follow font hierarchy and sizing guidelines
3. **Logos**: Use only official, unaltered logos from authorized sources
4. **Accessibility**: Ensure all implementations meet WCAG 2.0 AA standards

### Integration with Existing Applications

This brand package integrates with the brand externalization system:

```bash
# Mount the UM brand as external brand directory
docker run -v /path/to/um-brand:/app/external-brand your-app
```

The application will automatically use UM brand assets when mounted at the external brand path.

### Fallback Behavior

When external brand is not available, the application falls back to:
- Default Vaadin Lumo theme colors
- System fonts
- Generic application branding

## Development

### CSS Custom Properties

All brand elements are defined as CSS custom properties (variables) for easy customization and theming:

```css
:root {
  /* Colors */
  --um-maize: #FFCB05;
  --um-blue: #00274C;
  
  /* Typography */
  --um-font-primary: 'IBM Plex Sans', sans-serif;
  --um-font-size-base: 1rem;
  
  /* Brand mappings */
  --brand-primary: var(--um-maize);
  --brand-secondary: var(--um-blue);
}
```

### Responsive Design

Typography and spacing automatically adjust for different screen sizes while maintaining brand consistency.

### Accessibility

- All color combinations meet WCAG 2.0 AA contrast requirements
- Typography includes accessibility-focused font options
- Semantic HTML structure supported
- Screen reader friendly implementations

## Legal and Compliance

### Trademark Notice

University of Michigan logos, marks, and brand elements are protected by trademark and copyright laws. Use only with proper authorization.

### Contact Information

- **Brand Guidelines**: [brand.umich.edu](https://brand.umich.edu/)
- **Logo Requests**: [idstandards@umich.edu](mailto:idstandards@umich.edu)
- **Michigan Medicine Branding**: [branding.med.umich.edu](https://branding.med.umich.edu/)

## Updates and Maintenance

This brand implementation should be periodically reviewed against official University of Michigan brand guidelines to ensure continued compliance.

### Version History

- **v1.0.0**: Initial implementation based on 2024 brand guidelines

---

**University of Michigan | Michigan Medicine**  
*Transform lives through exceptional healthcare, research, and education*