# University of Michigan Brand Implementation Examples

Practical examples showing how to properly implement UM brand guidelines in web applications.

## HTML Implementation Examples

### Basic Brand Setup

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Michigan Medicine Application</title>
    
    <!-- UM Brand CSS -->
    <link rel="stylesheet" href="/um-brand/colors/brand-colors.css">
    <link rel="stylesheet" href="/um-brand/typography/brand-typography.css">
    
    <!-- Favicon -->
    <link rel="icon" type="image/svg+xml" href="/um-brand/visual-assets/icons/favicon.svg">
</head>
<body class="um-brand-body">
    <!-- Your content here -->
</body>
</html>
```

### Header with Official Branding

```html
<header class="um-header">
    <div class="um-header-content">
        <img src="/um-brand/logos/michigan-medicine-logo.svg" 
             alt="Michigan Medicine" 
             class="um-logo">
        <nav class="um-navigation">
            <a href="#" class="um-nav-link">Home</a>
            <a href="#" class="um-nav-link">Services</a>
            <a href="#" class="um-nav-link">Contact</a>
        </nav>
    </div>
</header>
```

## CSS Implementation Examples

### Component Styling with Brand Colors

```css
/* Primary Button */
.um-button-primary {
    background-color: var(--um-maize);
    color: var(--um-blue);
    border: 2px solid var(--um-maize);
    font-family: var(--um-font-primary);
    font-weight: var(--um-font-weight-semibold);
    padding: 12px 24px;
    border-radius: 4px;
    transition: all 0.2s ease;
}

.um-button-primary:hover {
    background-color: var(--brand-hover);
    border-color: var(--brand-hover);
}

.um-button-primary:focus {
    outline: 2px solid var(--brand-focus);
    outline-offset: 2px;
}

/* Secondary Button */
.um-button-secondary {
    background-color: transparent;
    color: var(--um-blue);
    border: 2px solid var(--um-blue);
    font-family: var(--um-font-primary);
    font-weight: var(--um-font-weight-semibold);
    padding: 12px 24px;
    border-radius: 4px;
}

.um-button-secondary:hover {
    background-color: var(--um-blue);
    color: var(--um-white);
}
```

### Typography Implementation

```css
/* Headers */
.um-heading-1 {
    font-family: var(--um-font-secondary); /* Montserrat */
    font-size: var(--um-font-size-4xl);
    font-weight: var(--um-font-weight-bold);
    color: var(--um-blue);
    line-height: 1.2;
    margin-bottom: 1rem;
}

.um-heading-2 {
    font-family: var(--um-font-secondary);
    font-size: var(--um-font-size-3xl);
    font-weight: var(--um-font-weight-semibold);
    color: var(--um-blue);
    line-height: 1.3;
    margin-bottom: 0.75rem;
}

/* Body Text */
.um-body-text {
    font-family: var(--um-font-body); /* Nunito Sans */
    font-size: var(--um-font-size-base);
    font-weight: var(--um-font-weight-normal);
    color: var(--um-black);
    line-height: 1.6;
    margin-bottom: 1rem;
}

/* Large Text for Accessibility */
.um-body-large {
    font-family: var(--um-font-accessible); /* Atkinson Hyperlegible */
    font-size: var(--um-font-size-lg);
    line-height: 1.7;
    color: var(--um-blue);
}
```

### Layout Examples

```css
/* Michigan Medicine Header */
.um-header {
    background-color: var(--um-blue);
    color: var(--um-white);
    padding: 1rem 0;
    border-bottom: 4px solid var(--um-maize);
}

.um-header-content {
    max-width: 1200px;
    margin: 0 auto;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 2rem;
}

.um-logo {
    height: 60px;
    width: auto;
}

/* Navigation */
.um-navigation {
    display: flex;
    gap: 2rem;
}

.um-nav-link {
    color: var(--um-white);
    text-decoration: none;
    font-family: var(--um-font-primary);
    font-weight: var(--um-font-weight-medium);
    padding: 0.5rem 1rem;
    border-radius: 4px;
    transition: background-color 0.2s ease;
}

.um-nav-link:hover {
    background-color: rgba(255, 203, 5, 0.1);
    color: var(--um-maize);
}

/* Card Component */
.um-card {
    background-color: var(--um-white);
    border: 1px solid var(--um-warm-gray);
    border-radius: 8px;
    padding: 2rem;
    box-shadow: 0 2px 8px rgba(0, 39, 76, 0.1);
    margin-bottom: 2rem;
}

.um-card-header {
    color: var(--um-blue);
    font-family: var(--um-font-secondary);
    font-size: var(--um-font-size-xl);
    font-weight: var(--um-font-weight-semibold);
    margin-bottom: 1rem;
    border-bottom: 2px solid var(--um-maize);
    padding-bottom: 0.5rem;
}
```

## Accessibility Implementation

### Focus Management

```css
/* Custom focus indicators using UM brand colors */
.um-focusable:focus {
    outline: 2px solid var(--um-cyan);
    outline-offset: 2px;
    box-shadow: 0 0 0 4px rgba(0, 178, 169, 0.2);
}

/* Skip link for keyboard navigation */
.um-skip-link {
    position: absolute;
    top: -40px;
    left: 6px;
    background: var(--um-blue);
    color: var(--um-white);
    padding: 8px;
    text-decoration: none;
    font-family: var(--um-font-primary);
    font-weight: var(--um-font-weight-semibold);
    border-radius: 4px;
    z-index: 1000;
}

.um-skip-link:focus {
    top: 6px;
}
```

### High Contrast Mode Support

```css
/* Enhanced contrast for accessibility */
@media (prefers-contrast: high) {
    .um-button-primary {
        border-width: 3px;
        font-weight: var(--um-font-weight-bold);
    }
    
    .um-card {
        border-width: 2px;
        border-color: var(--um-blue);
    }
}

/* Respect reduced motion preferences */
@media (prefers-reduced-motion: reduce) {
    .um-button-primary,
    .um-nav-link {
        transition: none;
    }
}
```

## React/JSX Examples

### Branded Button Component

```jsx
import React from 'react';
import './Button.css'; // includes UM brand styles

const UMButton = ({ 
    variant = 'primary', 
    size = 'medium', 
    children, 
    ...props 
}) => {
    const className = `um-button um-button-${variant} um-button-${size}`;
    
    return (
        <button className={className} {...props}>
            {children}
        </button>
    );
};

export default UMButton;
```

### Usage Example

```jsx
<UMButton variant="primary" onClick={handleSubmit}>
    Submit Application
</UMButton>

<UMButton variant="secondary" size="large">
    Learn More
</UMButton>
```

## Form Implementation

```html
<form class="um-form">
    <div class="um-form-group">
        <label for="name" class="um-label">Full Name</label>
        <input 
            type="text" 
            id="name" 
            class="um-input" 
            required 
            aria-describedby="name-help">
        <small id="name-help" class="um-help-text">
            Enter your first and last name
        </small>
    </div>
    
    <div class="um-form-group">
        <label for="email" class="um-label">Email Address</label>
        <input 
            type="email" 
            id="email" 
            class="um-input" 
            required>
    </div>
    
    <button type="submit" class="um-button-primary">
        Submit
    </button>
</form>
```

```css
.um-form-group {
    margin-bottom: 1.5rem;
}

.um-label {
    display: block;
    font-family: var(--um-font-primary);
    font-weight: var(--um-font-weight-semibold);
    color: var(--um-blue);
    margin-bottom: 0.5rem;
    font-size: var(--um-font-size-base);
}

.um-input {
    width: 100%;
    padding: 12px 16px;
    border: 2px solid var(--um-warm-gray);
    border-radius: 4px;
    font-family: var(--um-font-body);
    font-size: var(--um-font-size-base);
    color: var(--um-blue);
    background-color: var(--um-white);
    transition: border-color 0.2s ease;
}

.um-input:focus {
    outline: none;
    border-color: var(--um-cyan);
    box-shadow: 0 0 0 3px rgba(0, 178, 169, 0.2);
}

.um-help-text {
    font-family: var(--um-font-body);
    font-size: var(--um-font-size-sm);
    color: var(--um-dark-gray);
    margin-top: 0.25rem;
    display: block;
}
```

## Best Practices Summary

1. **Always use CSS custom properties** for colors and typography
2. **Maintain proper contrast ratios** for accessibility
3. **Use semantic HTML** with proper ARIA labels
4. **Test with keyboard navigation** and screen readers
5. **Respect user preferences** for motion and contrast
6. **Load official logos** from authorized sources only
7. **Follow spacing guidelines** for logo clearance
8. **Use recommended fonts** with appropriate fallbacks