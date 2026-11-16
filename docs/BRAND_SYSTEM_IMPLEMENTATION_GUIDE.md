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

All brand directories follow a consistent structure. This is the default brand, `elicit-brand/`:

```
elicit-brand/
├── brand-config.json          # Brand metadata (name, organization, logos, favicon)
├── brand-info.json            # Display metadata (description, primary color)
├── README.md
├── colors/
│   └── brand-colors.css       # CSS custom properties: palette and --brand-* mappings
├── typography/
│   ├── brand-typography.css   # @font-face rules, font stacks, type scale, utility classes
│   ├── typography.json        # Font metadata (for tooling and documentation)
│   └── fonts/                 # IBM Plex Sans, Sans Arabic and Mono (WOFF2), LICENSE.txt
├── images/
│   ├── favicon.ico            # Browser favicon
│   ├── HorizontalLogo.png     # The ELICIT / SURVEY lockup
│   ├── icon-white.png         # The white mark, shown in the application header
│   └── source/                # SVG masters the three files above are rendered from
└── theme.css                  # Imports colors and typography together
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

### 3. The Default Brand

The default brand is the one an application shows when a site mounts no brand of its own, and the
one a partial brand falls back to file by file.

#### A. Where It Lives

The source is the `elicit-brand/` directory of the umbrella repository, laid out as shown under
"Standardized Brand Directory Structure" above. `docker-compose.yml` mounts it read-only at
`/opt/brand` (the path given by `brand.file.system.path`).

Survey, Admin and Author each package a copy of that directory at
`src/main/resources/META-INF/brand/` as the built-in default. The mounted directory and the
embedded copies are the same files, except that the SVG masters under `images/source/` are not
embedded. The umbrella's `checkBrand.sh` keeps them so:

```bash
./checkBrand.sh           # fail when a copy differs from elicit-brand/
./checkBrand.sh --sync    # copy elicit-brand/ into the modules, then check
```

The check compares each module's packaged brand with `elicit-brand/` file by file (a module's
license header at the top of a stylesheet is not part of the comparison). It also checks the
palette of the manuals' style package, `docs/manual/elicit-brand.sty`: every
`\definecolor{elicit...}` there must name a color of `colors/brand-colors.css` and carry its
value. The `.sty` is edited by hand; `--sync` does not write it. `buildDockerImages.sh` runs the
check before it builds any image.

To change the default brand, edit `elicit-brand/`, run `./checkBrand.sh --sync`, and commit the
result in each module's repository.

#### B. Brand Colors
**Location**: `elicit-brand/colors/brand-colors.css`

The file defines the palette as `--elicit-*` custom properties and then maps the generic
`--brand-*` tokens, which the applications' own stylesheets consume, onto it:

```css
:root {
  --elicit-ink: #0A3D62;
  --elicit-verdigris: #22775D;
  --elicit-paper: #FDFCFA;
  /* ... */
  --brand-primary: var(--elicit-ink);
  --brand-primary-contrast: var(--elicit-paper);
  --brand-secondary: var(--elicit-verdigris);
  --brand-accent: var(--elicit-verdigris);
  --brand-background: var(--elicit-paper);
  --brand-text-primary: var(--elicit-charcoal);
  --brand-text-secondary: var(--elicit-slate);
}
```

| Color | Property | Value | Drives |
| ----- | -------- | ----- | ------ |
| Ink | `--elicit-ink` | `#0A3D62` | `--brand-primary`: header background, primary buttons, access-code pill text |
| Ink, dark | `--elicit-ink-dark` | `#072E4A` | Primary button hover, the border under the header |
| Verdigris | `--elicit-verdigris` | `#22775D` | `--brand-secondary`, `--brand-accent`, `--brand-focus`, `--brand-success`, `--brand-info`: secondary buttons, focus outlines |
| Verdigris, dark | `--elicit-verdigris-dark` | `#1C5F4B` | Secondary button hover |
| Verdigris, pale | `--elicit-verdigris-pale` | `#E3F3EE` | Access-code pill background |
| Paper | `--elicit-paper` | `#FDFCFA` | `--brand-background`, `--brand-base`, text on the primary color |
| White | `--elicit-white` | `#FFFFFF` | `--brand-surface`, text on the secondary color |
| Ledger | `--elicit-ledger` | `#E8E4DC` | `--brand-border` (`--elicit-ledger-light`, `#F0EDE6`, is `--brand-border-light`) |
| Charcoal | `--elicit-charcoal` | `#1A2229` | `--brand-text-primary`: heading and body text |
| Slate | `--elicit-slate` | `#4A5A6A` | `--brand-text-secondary`: question and field-label text |
| Brick | `--elicit-brick` | `#BB251B` | `--brand-error` |
| Amber | `--elicit-amber` | `#D9A02C` | `--brand-warning`, as a fill |
| Amber, text | `--elicit-amber-text` | `#8C6A1D` | `--brand-warning-text`: the warning color where it is drawn as text |

The file also defines `--elicit-ink-darker` (`#052238`), `--elicit-ink-darkest` (`#041A2B`) and
`--elicit-verdigris-light` (`#5CBBA4`), the 50% and 10% tints of the primary and status colors
(`--brand-primary-10pct` and so on), and the hover and active states (`--brand-hover`,
`--brand-active`). Its comments record the contrast ratio each text color was chosen for.

Besides the properties, the file carries a few rules: a flat Ink header with Paper text, solid
primary (Ink) and secondary (Verdigris) buttons, and the `.elicit-access-code` pill.

#### C. Brand Typography
**Location**: `elicit-brand/typography/brand-typography.css`, `elicit-brand/typography/fonts/`

The fonts are files in the brand, in WOFF2 format:

- **IBM Plex Sans**: regular, italic, medium, semibold, bold
- **IBM Plex Sans Arabic**: regular, medium, semibold, bold
- **IBM Plex Mono**: regular, semibold

`brand-typography.css` declares each one in an `@font-face` rule whose `url()` is relative to the
stylesheet:

```css
@font-face {
  font-family: "IBM Plex Sans";
  font-style: normal;
  font-weight: 400;
  font-display: swap;
  src: url("fonts/IBMPlexSans-Regular.woff2") format("woff2");
}
```

Each application inlines the stylesheet into the page and resolves the relative `url()` against
the brand, so the browser fetches the font from the application itself: under `/brand/` in Survey
and under `/api/brand/` in Admin and Author. No third-party font host is contacted. The Arabic
rules carry a `unicode-range`, so a page that shows no Arabic text does not fetch those files.

IBM Plex is licensed under the SIL Open Font License 1.1. The license travels with the fonts as
`typography/fonts/LICENSE.txt`.

The same file defines the font stacks (`--brand-font-primary`, `--brand-font-heading`,
`--brand-font-body`, `--brand-font-monospace`), each naming the IBM Plex family first and a system
font stack after it as the fallback, together with the weight, size and line-height scales
(`--brand-font-weight-*`, `--brand-font-size-*`, `--brand-line-height-*`) and a small set of
`.elicit-*` utility classes. `typography/typography.json` repeats the font metadata for tooling
and documentation.

A brand of your own can carry fonts the same way, or carry none and name a system font stack.

#### D. Brand Theme
**Location**: `elicit-brand/theme.css`

`theme.css` is the entry point the applications inline into the page `<head>`. In the default
brand it only imports the two stylesheets above:

```css
@import url('./colors/brand-colors.css');
@import url('./typography/brand-typography.css');
```

#### E. Brand Configuration
**Location**: `elicit-brand/brand-config.json`

```json
{
  "name": "Elicit Default Brand",
  "version": "2.0.0",
  "organization": "Elicit Software",
  "logos": {
    "horizontal": "HorizontalLogo.png",
    "icon": "icon-white.png"
  },
  "favicon": "favicon.ico",
  "localized": {
    "es-419": {
      "name": "Marca predeterminada de Elicit",
      "organization": "Elicit Software"
    },
    "ar": {
      "name": "العلامة الافتراضية لـ Elicit",
      "organization": "Elicit Software"
    }
  }
}
```

The optional `localized` block carries per-language variants of the name and organization, keyed
by language tag. `brand-info.json` beside it holds display metadata (description, primary color,
last update) and may carry a `localized` description inside its `"brand"` object.

The applications read three fixed image names from `images/`:

- `images/HorizontalLogo.png`: the full ELICIT / SURVEY lockup, shown on the System > Branding
  pages and in the manuals.
- `images/icon-white.png`: the white mark, shown in the application header on the primary color.
- The favicon named in `brand-config.json`, `images/favicon.ico` in the default brand (the white
  mark on an Ink tile at 16, 32 and 48 px).

The three files are rendered from the SVG masters in `images/source/`.

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
      accessCode.autoRegister: true
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
curl -s http://localhost:8081/api/brand/theme.css
curl -s http://localhost:8081/api/brand/brand-config.json
```

#### B. Test Logo Serving (Standardized images/ directory)
```bash
# Test Survey logo serving
curl -I http://localhost:8080/brand/images/HorizontalLogo.png
curl -I http://localhost:8080/brand/images/favicon.ico

# Test Admin logo serving
curl -I http://localhost:8081/api/brand/images/HorizontalLogo.png
curl -I http://localhost:8081/api/brand/images/favicon.ico
```

#### C. Test Partial Brand Fallback
```bash
# Mount partial brand (only has colors, missing images)
# Edit docker-compose.yml to use: - ./test-partial-brand/:/brand:ro
docker-compose up -d

# Verify fallback behavior - should serve embedded images
curl -I http://localhost:8080/brand/images/HorizontalLogo.png  # Should return 200 with embedded logo
curl -I http://localhost:8081/api/brand/images/HorizontalLogo.png  # Should return 200 with embedded logo

# Verify custom colors are loaded
curl -s http://localhost:8080/brand/colors/brand-colors.css  # Should return custom Halloween colors
curl -s http://localhost:8081/api/brand/colors/brand-colors.css  # Should return custom Halloween colors
```

#### D. Test Shared Brand Directory
```bash
# Verify both applications serve the same brand content
SURVEY_LOGO=$(curl -s http://localhost:8080/brand/images/HorizontalLogo.png | wc -c)
ADMIN_LOGO=$(curl -s http://localhost:8081/api/brand/images/HorizontalLogo.png | wc -c)

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
- **Solution**: Update any hardcoded logo paths to use `/brand/images/HorizontalLogo.png` (Survey) or `/api/brand/images/HorizontalLogo.png` (Admin)
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

## Who May Write a Brand

A brand is code as much as decoration. Its style sheets are served on every Survey and Admin page
and can restyle or hide anything there, so treat a brand directory like a release:

- Mount it read-only, as the shipped compose file does. No application writes to it.
- Let only the people who deploy the applications write to the directory on the host.
- Review a brand before mounting it, as you would any other change to the site.
- Ship the brand's fonts inside the brand (`typography/`) rather than importing them from another
  site with `@import url(...)`. A remote import sends every reader's address to that site and
  makes each page depend on it.

The same holds for the translations directory, which supplies the text of every page. See the
installation manual, chapter "Securing the site".

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
- **Consistent API**: Same URL structure below the brand prefix in every application (`/brand/` in Survey, `/api/brand/` in Admin and Author)
- **Simplified Structure**: Eliminated confusion between `/logos/` and `/visual-assets/` directories
- **Future-Proof**: Extensible system that can accommodate additional brand elements

Both Admin and Survey applications now support complete brand externalization with shared directory capabilities, robust fallback mechanisms, and a maintainable, secure architecture that follows established patterns from the working brand system implementations.