# University of Michigan Typography

Based on official brand guidelines from [brand.umich.edu](https://brand.umich.edu/design-resources/typography/)

## Typography Philosophy

Typography is a powerful tool. When used effectively, the right font commands attention, elicits emotions and above all, creates a voice. It's an essential component of the University of Michigan brand's visual identity.

## Font Recommendations

### General Use Typefaces

These fonts are recommended for general use across digital and print applications:

| Font Family | Use Case | Source |
|-------------|----------|--------|
| **IBM Plex Sans** | Primary sans-serif, UI elements | [Google Fonts](https://fonts.google.com/specimen/IBM+Plex+Sans) |
| **Montserrat** | Headers, navigation, emphasis | [Google Fonts](https://fonts.google.com/specimen/Montserrat) |
| **Nunito Sans** | Body text, readable content | [Google Fonts](https://fonts.google.com/specimen/Nunito+Sans) |
| **Merriweather** | Serif body text, formal documents | [Google Fonts](https://fonts.google.com/specimen/Merriweather) |

### Display Typefaces

For headlines and special emphasis:

| Font Family | Use Case | Source |
|-------------|----------|--------|
| **Playfair Display** | Large headers, editorial design | [Google Fonts](https://fonts.google.com/specimen/Playfair+Display) |
| **Oswald** | Condensed headers, signage | [Google Fonts](https://fonts.google.com/specimen/Oswald) |

### Accessibility Font

| Font Family | Use Case | Source |
|-------------|----------|--------|
| **Atkinson Hyperlegible** | Improved readability for visually impaired | [Google Fonts](https://fonts.google.com/specimen/Atkinson+Hyperlegible) |

*Named after the founder of the Braille Institute, Atkinson Hyperlegible was developed specifically for readers with low vision.*

## Typography Guidelines

### How to Choose a Typeface

#### Legibility
- Choose typefaces with conventional letterforms
- Choose typefaces with generous spacing
- Choose typefaces with a tall x-height (height of lowercase letters in relation to uppercase)

#### Readability
- Choose typefaces designed for your purpose
- Align text to "ragged right" for comfortable word spacing online
- Ensure leading (line spacing) is greater than the point size of your typeface

### Font Hierarchy

| Element | Font Family | Size | Weight | Color |
|---------|-------------|------|--------|-------|
| **Display** | Playfair Display | 60px | Bold | UM Blue |
| **H1** | Montserrat | 48px | Bold | UM Blue |
| **H2** | Montserrat | 36px | Semibold | UM Blue |
| **H3** | Montserrat | 30px | Semibold | UM Blue |
| **H4** | Montserrat | 24px | Medium | UM Blue |
| **H5** | Montserrat | 20px | Medium | UM Blue |
| **H6** | Montserrat | 18px | Medium | UM Blue |
| **Body Large** | Nunito Sans | 18px | Normal | Black |
| **Body** | Nunito Sans | 16px | Normal | Black |
| **Body Small** | Nunito Sans | 14px | Normal | Black |
| **Caption** | Nunito Sans | 12px | Normal | Warm Gray |

### Implementation

#### CSS Variables
```css
--um-font-primary: 'IBM Plex Sans', sans-serif;
--um-font-secondary: 'Montserrat', sans-serif;
--um-font-body: 'Nunito Sans', sans-serif;
--um-font-serif: 'Merriweather', serif;
--um-font-display: 'Playfair Display', serif;
--um-font-accessible: 'Atkinson Hyperlegible', sans-serif;
```

#### Typography Classes
- `.um-heading-1` through `.um-heading-6` for hierarchical headers
- `.um-body`, `.um-body-large`, `.um-body-small` for content
- `.um-caption` for supplementary text
- `.um-display` for large promotional text
- `.um-accessible` for improved accessibility

### Special Notes

#### Victors Font
The proprietary "Victors" font developed by Michigan Creative was specifically optimized for the words "University of Michigan". It was not designed for long-form reading and is therefore not available for general use.

#### Web Fonts
All recommended fonts are available through Google Fonts for easy web implementation. Fallback fonts are included for optimal cross-platform compatibility.

#### Responsive Considerations
Font sizes automatically adjust for mobile devices to maintain readability while preserving the visual hierarchy.