# Brand System Implementation Guide for Elicit Applications

> **Implementation Status**: ✅ **COMPLETED** - All core components have been implemented as of October 28, 2025.
> The brand system is fully functional with default branding and external brand mounting capabilities working for both Admin and Survey applications.

This document provides comprehensive instructions for the Elicit brand system that works across both Admin and Survey applications, supporting shared brand directories and external brand mounting.

## Elicit Brand System - Unified Implementation Guide

### Overview

This guide covers the unified brand system for both Admin and Survey applications. The brand system uses a simplified single-brand architecture that allows both applications to share the same brand directory:

1. **External Brand Mount** (`/brand/`) - Docker volume mounted brand directory shared between applications
2. **Embedded Defaults** - Fallback to embedded default resources when external brands are incomplete
3. **Standardized Structure** - Consistent directory layout across all brand implementations

### Key Features

- **Unified Architecture**: Both Admin and Survey applications use the same brand system
- **Shared Brand Directories**: A single brand directory can be mounted to both applications
- **Partial Brand Support**: Missing resources automatically fall back to embedded defaults
- **Standardized Structure**: All brands follow consistent `colors/`, `typography/`, `images/` structure
- **Enhanced Fallback**: Applications serve embedded resources when external brand files are missing

### Standardized Brand Directory Structure

All brand directories now follow a consistent structure:

```
brand-directory/
├── brand-config.json          # Brand configuration and metadata
├── colors/
│   ├── brand-colors.css      # CSS custom properties for colors
│   └── README.md             # Color usage guidelines
├── typography/               # Typography system
│   ├── brand-typography.css  # CSS typography definitions
│   └── README.md             # Typography guidelines
├── images/                   # All visual assets (logos, icons, etc.)
│   ├── HorizontalLogo.png    # Main horizontal logo for headers
│   ├── favicon.svg           # Browser favicon
│   └── README.md             # Image usage guidelines
└── theme.css                 # Vaadin integration styles
```

**Note**: The previous `logos/` and `visual-assets/` directories have been consolidated into `images/` for consistency.

### Architecture Summary

The brand system is designed to be:
- **Unified**: Same system works for both Admin (port 8081) and Survey (port 8080) applications
- **Shared**: Single brand directory can be mounted to multiple applications
- **Performant**: Efficient caching and resource serving via dual handler approach  
- **Secure**: Protection against path traversal attacks
- **Maintainable**: Clear separation of concerns and CSS variable contracts
- **Robust**: Enhanced fallback system with embedded default resources

## Implementation Steps

### 1. Brand Resource Handlers

Both Admin and Survey applications use enhanced resource handlers with embedded fallback support:

#### A. Enhanced BrandStaticFileFilter (Admin - Servlet Filter)
**Location**: `Admin/src/main/java/.../BrandStaticFileFilter.java`

The Admin application uses a servlet filter with embedded resource fallback:

```java
package com.elicitsoftware.admin.flow;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Enhanced servlet filter that serves brand assets with embedded resource fallback.
 * Serves from mounted /brand directory or falls back to embedded META-INF/resources.
 */
@WebFilter(urlPatterns = "/brand/*")
public class BrandStaticFileFilter implements Filter {

    private static final String BRAND_PATH = "/brand";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestURI = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String relativePath = requestURI.substring(contextPath.length());
        
        if (relativePath.startsWith("/brand/")) {
            String brandResource = relativePath.substring("/brand/".length());
            
            // Security: Prevent path traversal
            if (brandResource.contains("..") || brandResource.contains("//")) {
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            
            byte[] content = null;
            String contentType = null;
            
            // Try external brand directory first
            Path brandFile = Paths.get(BRAND_PATH, brandResource);
            if (Files.exists(brandFile) && Files.isRegularFile(brandFile)) {
                try {
                    content = Files.readAllBytes(brandFile);
                    contentType = getContentType(brandFile.getFileName().toString());
                } catch (IOException e) {
                    // Fall through to embedded resource
                }
            }
            
            // Fallback to embedded resources
            if (content == null) {
                content = readEmbeddedResource(brandResource);
                if (content != null) {
                    contentType = getContentType(brandResource);
                }
            }
            
            if (content != null) {
                httpResponse.setContentType(contentType);
                httpResponse.setHeader("Cache-Control", "public, max-age=3600");
                httpResponse.setContentLength(content.length);
                httpResponse.getOutputStream().write(content);
                httpResponse.getOutputStream().flush();
                return;
            }
        }
        
        chain.doFilter(request, response);
    }
    
    private byte[] readEmbeddedResource(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream("/META-INF/resources/" + resourcePath)) {
            if (is != null) {
                return is.readAllBytes();
            }
        } catch (IOException e) {
            // Resource not found in embedded resources
        }
        return null;
    }
    
    private String getContentType(String fileName) {
        String extension = getFileExtension(fileName);
        return switch (extension.toLowerCase()) {
            case "css" -> "text/css";
            case "js" -> "application/javascript";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "svg" -> "image/svg+xml";
            case "ico" -> "image/x-icon";
            case "woff" -> "font/woff";
            case "woff2" -> "font/woff2";
            case "ttf" -> "font/ttf";
            case "otf" -> "font/otf";
            case "json" -> "application/json";
            default -> "application/octet-stream";
        };
    }
    
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }
}
```

#### B. Enhanced BrandResourceHandler (Survey - JAX-RS Resource)
**Location**: `Survey/src/main/java/.../BrandResourceHandler.java`

The Survey application uses a JAX-RS resource with the same embedded fallback logic:

```java
package com.elicitsoftware.survey.flow;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.MediaType;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Enhanced JAX-RS resource for serving brand assets with embedded fallback.
 * Serves from mounted /brand directory or falls back to embedded META-INF/resources.
 */
@Path("/brand")
public class BrandResourceHandler {

    private static final String BRAND_PATH = "/brand";

    @GET
    @Path("/{resource:.*}")
    public Response getBrandResource(@PathParam("resource") String resource) {
        try {
            // Security: Prevent path traversal
            if (resource.contains("..") || resource.contains("//") || resource.startsWith("/")) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            byte[] content = null;
            String contentType = null;
            
            // Try external brand directory first
            java.nio.file.Path brandFile = Paths.get(BRAND_PATH, resource);
            if (Files.exists(brandFile) && Files.isRegularFile(brandFile)) {
                try {
                    content = Files.readAllBytes(brandFile);
                    contentType = Files.probeContentType(brandFile);
                } catch (Exception e) {
                    // Fall through to embedded resource
                }
            }
            
            // Fallback to embedded resources
            if (content == null) {
                content = readEmbeddedResource(resource);
                if (content != null) {
                    contentType = getContentTypeFromExtension(resource);
                }
            }
            
            if (content == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM;
            }
            
            return Response.ok(content, contentType)
                    .header("Cache-Control", "public, max-age=3600")
                    .build();
                    
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    private byte[] readEmbeddedResource(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream("/META-INF/resources/" + resourcePath)) {
            if (is != null) {
                return is.readAllBytes();
            }
        } catch (Exception e) {
            // Resource not found in embedded resources
        }
        return null;
    }
    
    private String getContentTypeFromExtension(String fileName) {
        // Same implementation as in BrandStaticFileFilter
        // ... (content type mapping logic)
    }
}
```

### 2. Enhanced BrandUtil Classes

Both applications include enhanced BrandUtil classes with updated logo paths:

#### Updated BrandUtil.java (Both Admin and Survey)
**Locations**: 
- `Admin/src/main/java/.../BrandUtil.java`
- `Survey/src/main/java/.../BrandUtil.java`

```java
package com.elicitsoftware.admin.flow; // or survey.flow

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Enhanced utility class for brand detection and resource management.
 * Updated to use standardized images/ directory structure.
 */
public class BrandUtil {

    private static final String BRAND_PATH = "/brand";

    /**
     * Detects the current brand based on the mounted brand directory.
     * 
     * @return The detected brand name, or "default-brand" if no brand is mounted
     */
    public static String detectCurrentBrand() {
        try {
            Path brandConfigPath = Paths.get(BRAND_PATH, "brand-config.json");
            if (Files.exists(brandConfigPath)) {
                String content = Files.readString(brandConfigPath);
                String brandName = extractJsonValue(content, "name");
                if (brandName != null && !brandName.trim().isEmpty()) {
                    return brandName.toLowerCase().replace(" ", "-");
                }
            }
        } catch (IOException e) {
            // Log error in production, fall through to default
        }
        
        return "default-brand";
    }

    /**
     * Gets the logo path using the standardized images/ directory structure.
     * 
     * @param logoName The logo filename (e.g., "HorizontalLogo.png")
     * @return The full path to the logo in the images directory
     */
    public static String getLogoPath(String logoName) {
        return "/brand/images/" + logoName;
    }

    /**
     * Gets the favicon path using the standardized images/ directory structure.
     * 
     * @return The path to the favicon in the images directory
     */
    public static String getFaviconPath() {
        return "/brand/images/favicon.svg";
    }

    /**
     * Generates a brand-specific title for the application.
     * 
     * @param baseTitle The base title of the application
     * @return The brand-specific title
     */
    public static String getBrandTitle(String baseTitle) {
        String currentBrand = detectCurrentBrand();
        if (!"default-brand".equals(currentBrand)) {
            return baseTitle + " - " + formatBrandName(currentBrand);
        }
        return baseTitle;
    }

    /**
     * Formats a brand name for display (converts kebab-case to Title Case).
     * 
     * @param brandName The brand name in kebab-case
     * @return The formatted brand name
     */
    private static String formatBrandName(String brandName) {
        return brandName.substring(0, 1).toUpperCase() + 
               brandName.substring(1).replace("-", " ");
    }

    /**
     * Simple JSON value extraction without full JSON parsing.
     * 
     * @param json The JSON string to extract from
     * @param key The key to extract
     * @return The extracted value, or null if not found
     */
    private static String extractJsonValue(String json, String key) {
        try {
            String searchPattern = "\"" + key + "\"";
            int keyIndex = json.indexOf(searchPattern);
            if (keyIndex == -1) return null;
            
            int colonIndex = json.indexOf(":", keyIndex);
            if (colonIndex == -1) return null;
            
            int valueStart = json.indexOf("\"", colonIndex);
            if (valueStart == -1) return null;
            valueStart++; // Skip the opening quote
            
            int valueEnd = json.indexOf("\"", valueStart);
            if (valueEnd == -1) return null;
            
            return json.substring(valueStart, valueEnd);
        } catch (Exception e) {
            return null;
        }
    }
}
```

### 3. Standardized Default Brand Structure

Both applications include default brand directories with the standardized structure:

#### A. Create Brand Directory Structure
```bash
# For Admin application
mkdir -p Admin/src/main/resources/META-INF/resources/colors
mkdir -p Admin/src/main/resources/META-INF/resources/typography  
mkdir -p Admin/src/main/resources/META-INF/resources/images

# For Survey application
mkdir -p Survey/src/main/resources/META-INF/resources/colors
mkdir -p Survey/src/main/resources/META-INF/resources/typography
mkdir -p Survey/src/main/resources/META-INF/resources/images
```

#### B. Standardized Brand Colors
**Location**: Both `Admin/src/main/resources/META-INF/resources/colors/brand-colors.css` and `Survey/src/main/resources/META-INF/resources/colors/brand-colors.css`

```css
/*
   Elicit Admin Application Theme
   
   This theme provides:
   - Application-specific component styling
   - Brand integration architecture via CSS custom properties
   - Base Vaadin Lumo theme integration
   
   Brand colors and typography are provided by:
   - Default: ./brand/ directory (copied into Docker image)
   - External: Mounted brand directories (e.g., um-brand, test-brand)
   
   Visit https://vaadin.com/docs/latest/styling/application-theme and
   https://vaadin.com/docs/latest/styling/lumo for more information.
*/

/* Import local fonts as fallback */
@import 'fonts/style.css';

/* Brand Integration Architecture */
/* These CSS custom properties provide the contract between brands and the application */
html {
    /* Brand variables are populated by brand CSS files */
    /* Mapping brand variables to Lumo theme properties */
    --lumo-font-family: var(--brand-font-primary, -apple-system, BlinkMacSystemFont, "Roboto", "Segoe UI", Helvetica, Arial, sans-serif);
    
    /* Primary color mapping */
    --lumo-primary-color: var(--brand-primary, hsl(214, 90%, 52%));
    --lumo-primary-text-color: var(--brand-primary, hsl(214, 90%, 52%));
    --lumo-primary-contrast-color: var(--brand-primary-contrast, hsl(0, 0%, 100%));
    --lumo-base-color: var(--brand-base, hsl(0, 0%, 100%));
    
    /* Primary color variants */
    --lumo-primary-color-50pct: var(--brand-primary-50pct, hsla(214, 90%, 52%, 0.5));
    --lumo-primary-color-10pct: var(--brand-primary-10pct, hsla(214, 90%, 52%, 0.1));
    
    /* Status color mapping */
    --lumo-error-color: var(--brand-error, hsl(3, 85%, 48%));
    --lumo-error-text-color: var(--brand-error, hsl(3, 85%, 48%));
    --lumo-error-color-50pct: var(--brand-error-50pct, hsla(3, 85%, 48%, 0.5));
    --lumo-error-color-10pct: var(--brand-error-10pct, hsla(3, 85%, 48%, 0.1));
    
    --lumo-success-color: var(--brand-success, hsl(145, 72%, 30%));
    --lumo-success-text-color: var(--brand-success, hsl(145, 72%, 30%));
    --lumo-success-color-50pct: var(--brand-success-50pct, hsla(145, 72%, 30%, 0.5));
    --lumo-success-color-10pct: var(--brand-success-10pct, hsla(145, 72%, 30%, 0.1));
    
    --lumo-warning-color: var(--brand-warning, hsl(43, 100%, 48%));
    --lumo-warning-text-color: var(--brand-warning, hsl(43, 100%, 48%));
    --lumo-warning-color-50pct: var(--brand-warning-50pct, hsla(43, 100%, 48%, 0.5));
    --lumo-warning-color-10pct: var(--brand-warning-10pct, hsla(43, 100%, 48%, 0.1));
}

/* Application-specific styling (customize for Admin app) */
.main-layout {
    /* Your existing admin-specific styles */
}

/* Ensure proper brand integration for common components */
vaadin-button[theme="primary"] {
    background-color: var(--lumo-primary-color);
    color: var(--lumo-primary-contrast-color);
}

vaadin-button[theme="primary"]:hover {
    background-color: var(--lumo-primary-color-50pct);
}

/* Add any Admin-specific component styling here */
```

### 3. Create Default Brand Directory

#### A. Create Brand Directory Structure
```bash
mkdir -p brand/colors
mkdir -p brand/typography
mkdir -p brand/visual-assets/icons
mkdir -p brand/logos
```

#### B. Brand Colors
**Location**: `brand/colors/brand-colors.css`

```css
:root {
  /* Primary Brand Colors - Vaadin Lumo Standards */
  --brand-primary: hsl(214, 90%, 52%); /* Vaadin Lumo primary blue */
  --brand-primary-hsl: 214, 90%, 52%;
  --brand-primary-contrast: hsl(0, 0%, 100%); /* White text on primary */
  --brand-primary-50pct: hsla(214, 90%, 52%, 0.5);
  --brand-primary-10pct: hsla(214, 90%, 52%, 0.1);
  --brand-base: hsl(0, 0%, 100%); /* White base */

  /* Status Colors - Vaadin Lumo Standards */
  --brand-error: hsl(3, 85%, 48%); /* Lumo error red */
  --brand-error-hsl: 3, 85%, 48%;
  --brand-error-50pct: hsla(3, 85%, 48%, 0.5);
  --brand-error-10pct: hsla(3, 85%, 48%, 0.1);
  --brand-success: hsl(145, 72%, 30%); /* Lumo success green */
  --brand-success-hsl: 145, 72%, 30%;
  --brand-success-50pct: hsla(145, 72%, 30%, 0.5);
  --brand-success-10pct: hsla(145, 72%, 30%, 0.1);
  --brand-warning: hsl(43, 100%, 48%); /* Lumo warning yellow */
  --brand-warning-hsl: 43, 100%, 48%;
  --brand-warning-50pct: hsla(43, 100%, 48%, 0.5);
  --brand-warning-10pct: hsla(43, 100%, 48%, 0.1);

  /* Interactive Colors - Following Vaadin Standards */
  --brand-interactive: hsl(214, 90%, 52%); /* Same as primary */
  --brand-interactive-rgb: 22, 118, 243;
  --brand-interactive-light: rgba(22, 118, 243, 0.1);

  /* Typography */
  --brand-font-primary: -apple-system, BlinkMacSystemFont, "Roboto", "Segoe UI", Helvetica, Arial, sans-serif;
  --brand-font-secondary: var(--brand-font-primary);
  --brand-font-heading: var(--brand-font-primary);
  --brand-font-body: var(--brand-font-primary);
  --brand-font-monospace: "Consolas", "Monaco", "Courier New", monospace;

  /* Semantic Mappings */
  --brand-background: var(--brand-base);
  --brand-surface: var(--brand-base);
  --brand-text-primary: hsl(214, 35%, 15%);
  --brand-text-secondary: hsl(214, 25%, 35%);
  --brand-text-disabled: hsl(214, 15%, 55%);
  
  /* Border and Shadow Colors */
  --brand-border: hsl(214, 20%, 85%);
  --brand-border-light: hsl(214, 20%, 92%);
  --brand-shadow: hsla(214, 40%, 20%, 0.1);
  
  /* Interactive States */
  --brand-hover: hsla(214, 90%, 52%, 0.1);
  --brand-active: hsla(214, 90%, 52%, 0.2);
  --brand-focus: hsl(214, 90%, 52%);
}
```

#### C. Brand Typography
**Location**: `brand/typography/brand-typography.css`

```css
/* Elicit Admin - Default Typography */
/* Uses system font stack for optimal performance and compatibility */

@import url('./font-declarations.css');

:root {
  /* Font Family Definitions */
  --brand-font-primary: -apple-system, BlinkMacSystemFont, "Roboto", "Segoe UI", Helvetica, Arial, sans-serif;
  --brand-font-secondary: var(--brand-font-primary);
  --brand-font-heading: var(--brand-font-primary);
  --brand-font-body: var(--brand-font-primary);
  --brand-font-monospace: "SF Mono", "Monaco", "Inconsolata", "Roboto Mono", "Consolas", "Courier New", monospace;
  
  /* Font Weights */
  --brand-font-weight-light: 300;
  --brand-font-weight-normal: 400;
  --brand-font-weight-medium: 500;
  --brand-font-weight-semibold: 600;
  --brand-font-weight-bold: 700;
  
  /* Font Sizes */
  --brand-font-size-xs: 0.75rem;    /* 12px */
  --brand-font-size-sm: 0.875rem;   /* 14px */
  --brand-font-size-base: 1rem;     /* 16px */
  --brand-font-size-lg: 1.125rem;   /* 18px */
  --brand-font-size-xl: 1.25rem;    /* 20px */
  --brand-font-size-2xl: 1.5rem;    /* 24px */
  --brand-font-size-3xl: 1.875rem;  /* 30px */
  --brand-font-size-4xl: 2.25rem;   /* 36px */
  
  /* Line Heights */
  --brand-line-height-tight: 1.25;
  --brand-line-height-normal: 1.5;
  --brand-line-height-relaxed: 1.75;
}

/* Apply brand typography to common elements */
body {
  font-family: var(--brand-font-body);
  font-weight: var(--brand-font-weight-normal);
  line-height: var(--brand-line-height-normal);
}

h1, h2, h3, h4, h5, h6 {
  font-family: var(--brand-font-heading);
  font-weight: var(--brand-font-weight-semibold);
  line-height: var(--brand-line-height-tight);
}

code, pre {
  font-family: var(--brand-font-monospace);
}
```

#### D. Brand Theme
**Location**: `brand/theme.css`

```css
/* Elicit Admin - Default Brand Theme */
/* Integrates brand colors and typography with Vaadin Lumo theme */

@import url('./colors/brand-colors.css');
@import url('./typography/brand-typography.css');

/* Brand-specific customizations for Admin application */
:root {
  /* Admin-specific brand variables can be defined here */
  --admin-sidebar-bg: var(--brand-surface);
  --admin-header-bg: var(--brand-primary);
  --admin-content-bg: var(--brand-background);
}

/* Admin application specific styling */
.admin-layout {
  background-color: var(--admin-content-bg);
}

.admin-sidebar {
  background-color: var(--admin-sidebar-bg);
  border-right: 1px solid var(--brand-border);
}

.admin-header {
  background-color: var(--admin-header-bg);
  color: var(--brand-primary-contrast);
}

/* Ensure proper integration with Vaadin components */
vaadin-app-layout[theme~="admin"] {
  --lumo-base-color: var(--brand-base);
  --lumo-primary-color: var(--brand-primary);
}
```

#### E. Brand Configuration
**Location**: `brand/brand-config.json`

```json
{
  "brand": {
    "name": "Elicit Admin Application - Default Brand",
    "version": "1.0.0",
    "description": "Default brand configuration using Vaadin standard colors and Elicit styling"
  },
  "structure": {
    "colors": {
      "directory": "colors/",
      "css": "colors/brand-colors.css",
      "metadata": "colors/palette.json"
    },
    "logos": {
      "directory": "logos/",
      "stacked": "logos/stacked.png"
    },
    "typography": {
      "directory": "typography/",
      "css": "typography/brand-typography.css",
      "metadata": "typography/typography.json",
      "fonts": {
        "system": "System font stack (Roboto, Segoe UI, etc.)"
      }
    },
    "icons": {
      "directory": "visual-assets/icons/",
      "favicon": "visual-assets/icons/favicon.ico",
      "favicon32": "visual-assets/icons/favicon-32x32.png"
    }
  },
  "deployment": {
    "docker": {
      "volumeMount": "./brand:/brand",
      "example": "docker run -v ./custom-brand:/brand elicitsoftware/admin:latest"
    },
    "kubernetes": {
      "configMap": "Mount brand files as ConfigMap to /brand",
      "example": "See README.md for complete Kubernetes configuration"
    }
  },
  "customization": {
    "colors": "Modify colors/brand-colors.css to override default brand colors",
    "fonts": "Replace font files in typography/ and update brand-typography.css",
    "logos": "Replace image files in logos/ directory",
    "icons": "Replace favicon files in visual-assets/icons/"
  }
}
```

### 4. Docker Integration for Shared Brand Directories

The unified brand system supports shared brand directories that can be mounted to both applications:

#### A. Docker Compose Configuration - Shared Brand Mount
**Location**: `docker-compose.yml`

Both applications can mount the same brand directory:

```yaml
services:
  survey:
    image: elicitsoftware/survey:latest
    ports:
      - "8080:8080"
    volumes:
      # Shared brand directory mounted to both applications
      - ./um-brand/:/brand:ro
      # Alternative brand options:
      # - ./test-brand/:/brand:ro
      # - ./test-partial-brand/:/brand:ro
    environment:
      TZ: America/Detroit
      token.autoRegister: true
    depends_on:
     - db

  admin:
    image: elicitsoftware/admin:latest
    ports:
      - "8081:8080"
    volumes:
      # Same brand directory mounted to admin
      # - ./test-brand/:/brand:ro
      # Alternative brand options:
      # - ./test-partial-brand/:/brand:ro
    environment:
      TZ: America/Detroit
      quarkus.mailer.from: elicit@localhost
      quarkus.mailer.host: mailpit
      quarkus.mailer.port: 1025
      quarkus.mailer.tls: false
    depends_on:
      - db
      - survey
```

#### B. Brand Switching Examples

Switch brands for both applications simultaneously:

```bash
# Use UM brand for both applications
docker-compose down
# Edit docker-compose.yml to uncomment: - ./um-brand/:/brand:ro
docker-compose up -d

# Use test brand for both applications  
docker-compose down
# Edit docker-compose.yml to uncomment: - ./test-brand/:/brand:ro
docker-compose up -d

# Use partial brand (inherits missing resources from embedded defaults)
docker-compose down
# Edit docker-compose.yml to uncomment: - ./test-partial-brand/:/brand:ro
docker-compose up -d
```

#### C. Individual Brand Assignment

You can also assign different brands to each application:

```yaml
services:
  survey:
    # ... other config
    volumes:
      - ./test-brand/:/brand:ro  # Healthcare colors for Survey

  admin:
    # ... other config  
    volumes:
      - ./um-brand/:/brand:ro    # UM colors for Admin
```

### 5. Testing and Verification

#### A. Test Brand Detection (Both Applications)
```bash
# Test Survey application brand detection (port 8080)
curl -s http://localhost:8080/ | grep brand-info
curl -s http://localhost:8080/brand/theme.css
curl -s http://localhost:8080/brand/brand-config.json

# Test Admin application brand detection (port 8081)  
curl -s http://localhost:8081/ | grep brand-info
curl -s http://localhost:8081/brand/theme.css
curl -s http://localhost:8081/brand/brand-config.json
```

#### B. Test Logo Serving (Standardized images/ directory)
```bash
# Test Survey logo serving
curl -I http://localhost:8080/brand/images/HorizontalLogo.png
curl -I http://localhost:8080/brand/images/favicon.svg

# Test Admin logo serving
curl -I http://localhost:8081/brand/images/HorizontalLogo.png
curl -I http://localhost:8081/brand/images/favicon.svg
```

#### C. Test Partial Brand Fallback
```bash
# Mount partial brand (only has colors, missing images)
# Edit docker-compose.yml to use: - ./test-partial-brand/:/brand:ro
docker-compose up -d

# Verify fallback behavior - should serve embedded images
curl -I http://localhost:8080/brand/images/HorizontalLogo.png  # Should return 200 with embedded logo
curl -I http://localhost:8081/brand/images/HorizontalLogo.png  # Should return 200 with embedded logo

# Verify custom colors are loaded
curl -s http://localhost:8080/brand/colors/brand-colors.css  # Should return custom Halloween colors
curl -s http://localhost:8081/brand/colors/brand-colors.css  # Should return custom Halloween colors
```

#### D. Test Shared Brand Directory
```bash
# Verify both applications serve the same brand content
SURVEY_LOGO=$(curl -s http://localhost:8080/brand/images/HorizontalLogo.png | wc -c)
ADMIN_LOGO=$(curl -s http://localhost:8081/brand/images/HorizontalLogo.png | wc -c)

echo "Survey logo size: $SURVEY_LOGO bytes"
echo "Admin logo size: $ADMIN_LOGO bytes"
# Should show identical file sizes when using shared brand directory
```

### 6. Implementation Verification Checklist

#### Admin Application
- [x] Enhanced BrandStaticFileFilter.java with embedded resource fallback
- [x] Enhanced BrandResourceHandler.java for JAX-RS resource serving (if used)
- [x] Updated BrandUtil.java with standardized images/ directory paths
- [x] Application theme updated with CSS variable contracts
- [x] Docker volume mount configured to `/brand` directory
- [x] Embedded default resources in META-INF/resources/
- [x] Brand system working with mounted brands (✅ verified working)
- [x] Logo serving from images/ directory (✅ verified working) 
- [x] Fallback to embedded resources when external brand incomplete (✅ verified working)

#### Survey Application  
- [x] Enhanced BrandResourceHandler.java with embedded resource fallback
- [x] Updated BrandUtil.java with standardized images/ directory paths
- [x] Application theme updated with CSS variable contracts
- [x] Docker volume mount configured to `/brand` directory
- [x] Embedded default resources in META-INF/resources/
- [x] Brand system working with mounted brands (✅ verified working)
- [x] Logo serving from images/ directory (✅ verified working)
- [x] Fallback to embedded resources when external brand incomplete (✅ verified working)

#### Unified System
- [x] Shared brand directory mounting working for both applications
- [x] Standardized brand directory structure (colors/, typography/, images/)
- [x] Partial brand support with automatic fallback
- [x] Brand switching capability via docker-compose.yml volume mounts
- [x] Both applications can serve the same brand simultaneously

### 7. Common Issues and Solutions

#### Brand Not Loading
- **Issue**: Volume mount not working
- **Solution**: Verify volume mount syntax: `-v ./brand-dir:/brand:ro`
- **Solution**: Check that brand-config.json exists in the mounted directory
- **Solution**: Completely remove and recreate Docker containers after volume mount changes:
  ```bash
  docker-compose stop admin
  docker-compose rm -f admin  
  docker-compose up -d admin
  ```

#### Images Not Displaying
- **Issue**: Logo paths pointing to old `/logos/` directory
- **Solution**: Ensure all brand directories use standardized structure with `images/` directory
- **Solution**: Update any hardcoded logo paths to use `/brand/images/HorizontalLogo.png`
- **Solution**: Verify BrandUtil.getLogoPath() method returns `/brand/images/` paths

#### CSS Not Applying  
- **Issue**: Brand CSS files missing or not loading
- **Solution**: Ensure brand CSS files exist in mounted directory:
  - `/brand/colors/brand-colors.css`
  - `/brand/typography/brand-typography.css`
  - `/brand/theme.css`
- **Solution**: Clear browser cache (Cmd+Shift+R)
- **Solution**: Verify CSS variable names match Vaadin Lumo contracts

#### Partial Brand Not Working
- **Issue**: Missing resources not falling back to embedded defaults
- **Solution**: Verify embedded resources exist in both applications:
  - `Admin/src/main/resources/META-INF/resources/images/HorizontalLogo.png`
  - `Survey/src/main/resources/META-INF/resources/images/HorizontalLogo.png`
- **Solution**: Ensure fallback logic is implemented in both BrandStaticFileFilter and BrandResourceHandler

#### Different File Sizes Between Applications
- **Issue**: Applications serving different logo files when using shared brand
- **Solution**: Verify both containers are mounting the same brand directory
- **Solution**: Check that both applications are using enhanced resource handlers with fallback
- **Solution**: Test with `curl -I` to compare Content-Length headers

## Summary

This unified brand system implementation provides:

### ✅ **Completed Features**
- **Unified Architecture**: Both Admin (port 8081) and Survey (port 8080) applications use the same brand system
- **Shared Brand Directories**: Single brand directory can be mounted to multiple applications simultaneously
- **Enhanced Fallback System**: Embedded resource fallback when external brand resources are missing or incomplete
- **Standardized Structure**: All brands follow consistent `colors/`, `typography/`, `images/` directory layout
- **Dual Resource Handlers**: Servlet filter (Admin) and JAX-RS resource (Survey) with identical functionality
- **Security**: Path traversal protection in all resource handlers
- **Performance**: Proper caching headers and efficient resource serving
- **Partial Brand Support**: Incomplete brands automatically inherit missing resources from embedded defaults

### ✅ **Verified Working Examples**
- **um-brand**: University of Michigan brand with complete colors, typography, and images
- **test-brand**: Healthcare-themed brand with Covenant Healthcare inspired colors
- **test-partial-brand**: Halloween-themed partial brand (only colors) with automatic image fallback

### ✅ **Deployment Flexibility**
- **Docker Volume Mounts**: Easy brand switching via docker-compose.yml configuration
- **Container Sharing**: Same brand directory mounted to both applications for consistent branding
- **Individual Assignment**: Different brands can be assigned to each application if needed
- **Development Mode**: Embedded defaults provide working baseline without external brand mounting

### ✅ **Maintenance Benefits**
- **Clean Architecture**: Clear separation between application logic and brand resources
- **Consistent API**: Same `/brand/` URL structure across all applications
- **Simplified Structure**: Eliminated confusion between `/logos/` and `/visual-assets/` directories
- **Future-Proof**: Extensible system that can accommodate additional brand elements

Both Admin and Survey applications now support complete brand externalization with shared directory capabilities, robust fallback mechanisms, and a maintainable, secure architecture that follows established patterns from the working brand system implementations.