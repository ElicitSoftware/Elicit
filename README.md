<a id="readme-top"></a>

<!-- PROJECT SHIELDS -->
<!--
*** I'm using markdown "reference style" links for readability.
*** Reference links are enclosed in brackets [ ] instead of parentheses ( ).
*** See the bottom of this document for the declaration of the reference variables
*** for contributors-url, forks-url, etc. This is an optional, concise syntax you may use.
*** https://www.markdownguide.org/basic-syntax/#reference-style-links
-->
<!-- [![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![project_license][license-shield]][https://polyformproject.org/licenses/noncommercial/1.0.0]
[![LinkedIn][linkedin-shield]][linkedin-url] -->

<!-- Powered by Michigan -->
<br />
<div align="center">
  <a href="https://innovationpartnerships.umich.edu/">
    <img src="https://github.com/ElicitSoftware/Elicit/blob/main/images/stacked.png" alt="Logo" width="15%" >
  </a>
<h3 align="center">Elicit Software</h3>
  <p>Copyright © 2025–2026 The Regents of the University of Michigan</p>
</div>
<!-- TABLE OF CONTENTS -->
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li><a href="#built-with">Built With</a></li>
    <li><a href="#trying-elicit">Trying Elicit</a></li>
    <li><a href="#a-guided-tour">A Guided Tour</a></li>
    <li><a href="#going-further">Going Further</a></li>
    <!-- <li><a href="#roadmap">Roadmap</a></li> -->
    <li><a href="#contributing">Contributing</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#contact">Contact</a></li>
    <li><a href="#acknowledgments">Acknowledgments</a></li>
  </ol>
</details>

<!-- ABOUT THE PROJECT -->
## About The Project
  <p align="left">
    Elicit Software was designed and developed by <a href="https://www.michiganmedicine.org/">Michigan Medicine</a>. It is a modular system to build and deliver complex surveys with custom reporting.<br/>
    The modules include:
    <ul>
    <li><a href="https://github.com/ElicitSoftware/Admin/">Admin</a><br/>
      <ul>
      <li>The Admin application is used by survey administrators to register subjects, view survey progress, and view final reports.</li>
      </ul>
    </li>
    <li><a href="https://github.com/ElicitSoftware/Author/">Author</a><br/>
      <ul>
        <li>The authoring tool: survey authors design steps, sections, questions, rules, reports and translations, preview them, and export a survey definition file (<code>.elicit</code>).</li>
        <li>Sites that only deliver surveys do not need it; it has its own installation manual.</li>
      </ul>
    </li>
    <li><a href="https://github.com/ElicitSoftware/Survey/">Survey</a><br/>
      <ul>
        <li>The subject-facing application that presents questions, guides users through the decision tree, records answers, and produces final reports.</li>
      </ul>
    </li>
    <li><a href="https://github.com/ElicitSoftware/FHHS/">Family Health History Survey (FHHS)</a><br/>
      <ul>
       <li>A fully functioning Elicit Survey created to support the <a href="https://info.mightstudy.org/">Michigan Genetic Hereditary Testing Study</a>.</li>
       <li>The FHHS has been shared under an open source license for all cancer oncology practices to use and adapt.</li>
      </ul>
    </li>
    <li><a href="https://github.com/ElicitSoftware/Pedigree/">Pedigree</a><br/>
      <ul>
        <li>A Docker container to serve the <a href="https://bioconductor.org/packages/Pedixplorer/">Pedixplorer</a> R package. This container supports the visual drawing of family pedigrees for the FHHS Survey.</li>
      </ul>
    </li>
    <li><a href="https://github.com/ElicitSoftware/PostgreSQL/">PostgreSQL Database</a><br/>
      <ul>
        <li>An open source object-relational database used to store decision tree logic and data.</li>
      </ul>
    </li>
    </ul>
    Surveys themselves are designed in Author, which exports a survey definition file
    (<code>.elicit</code>) that the Admin application applies to a site. The Family Health History Survey and the sample under <code>samples/</code> are two such
    definitions, ready to apply.
  </p>

<!-- [![Product Name Screen Shot][product-screenshot]](https://example.com)

Here's a blank template to get started. To avoid retyping too much info, do a search and replace with your text editor for the following: `github_username`, `repo_name`, `twitter_handle`, `linkedin_username`, `email_client`, `email`, `project_title`, `project_description`, `project_license`
 -->

<p align="right">(<a href="#readme-top">back to top</a>)</p>

### Built With

[![Java][Java]][Java-url]
[![Quarkus][Quarkus.io]][Quarkus-url]
[![Vaadin][Vaadin.com]][Vaadin-url]
[![Postgresql][Postgresql.com]][Postgresql-url]
[![Maven][Maven.org]][Maven-url]
[![Docker][Docker.com]][Docker-url]

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- GETTING STARTED -->
## Trying Elicit
To try Elicit you need one file, [`docker-compose.standalone.yml`](docker-compose.standalone.yml),
and the published Docker images. Nothing has to be compiled, and you don't need git or any of
the repositories.

You need Docker Desktop (or Docker Engine with Compose v2.23 or later) and these ports free:
8080–8082, 8084, 8085, 8180, 8025 and 1025 (Mailpit), and 16686, 4317 and 4318 (Jaeger).

1. Download the file into an empty folder:

   ```bash
   curl -fLO https://raw.githubusercontent.com/ElicitSoftware/Elicit/main/docker-compose.standalone.yml
   ```

2. In that folder, start the stack:

   ```bash
   docker compose -f docker-compose.standalone.yml up -d
   docker compose -f docker-compose.standalone.yml ps
   ```

The file runs every image at the release it comes with: the applications, the database and the
identity provider, all under one version number. To run another release, set `ELICIT_TAG`, for
example `ELICIT_TAG=3.0.0-rc.2 docker compose -f docker-compose.standalone.yml up -d`.

The first start pulls the images and creates the database, which takes a few minutes. `ps` shows
each service as *healthy* once it is ready. FHHS stays *unhealthy* until the Family Health History
Survey is applied (step 2 of the tour below); that is expected.

| Application              | Address                  | Sign in as                       |
| ------------------------ | ------------------------ | -------------------------------- |
| Admin                    | http://localhost:8081    | `admin` / `admin`                |
| Survey                   | http://localhost:8080    | any access code (tour step 3)    |
| Author                   | http://localhost:8084    | `author` / `author`              |
| Mailpit (sent email)     | http://localhost:8025    | —                                |
| Keycloak (identity)      | http://localhost:8180    | `admin` / `admin`, master realm  |
| Jaeger (traces)          | http://localhost:16686   | —                                |

This stack is for evaluation only: it is neither a development setup nor a production
installation. Its passwords are published, its ports are open, and it should never hold real
respondent data. It shows the default Elicit brand in English. A real site is installed against
its own database, identity provider and network, as the installation manual describes (see
[Going further](#going-further)).

<!-- USAGE EXAMPLES -->
## A Guided Tour
A new site starts with no survey. A survey travels as a **survey definition file** (`.elicit`),
and two are ready to apply:

- [`samples/census-household-survey.elicit`](samples/census-household-survey.elicit), in this
  repository: a small demonstration survey that uses every question type and rule the engine
  supports.
- [`family-history-survey.elicit`](https://github.com/ElicitSoftware/FHHS/blob/main/family-history-survey.elicit),
  in the FHHS repository: the Family Health History Survey, with its custom reports and pedigree
  drawing. Use the **Download raw file** button on that page to save it.

1. **Sign in to Admin** at http://localhost:8081 as `admin`. A dialog asks for a department
   first: follow its link to **Departments** and create one.
2. **Apply a survey** under **Apply Survey Definition**, uploading one of the files above. The
   survey and its reporting schema are built as part of the apply; nothing needs restarting.
3. **Take the survey.** The evaluation stack lets Survey accept any access code you make up, so
   open http://localhost:8080, choose the survey and type any code.
4. **Or follow the real path:** register a subject in Admin (one at a time, in bulk, or over the
   integration API), generate an access code and send the invitation. The email arrives in
   Mailpit at http://localhost:8025; its link opens Survey with the code filled in.
5. **Review the results** in Admin: survey progress for each subject and the final reports.
6. **Design a survey** in Author at http://localhost:8084: build steps, sections, questions and
   rules, preview them as a respondent would, and export a `.elicit` file for step 2.

`docker compose -f docker-compose.standalone.yml down` stops the stack and keeps its data. To
start over with an empty database, add `-v`, which deletes the data, and bring the stack up again.

<!-- GOING FURTHER -->
## Going Further
- **Installing a real site:** the installation manual is published as a PDF with each release on
  the [releases page](https://github.com/ElicitSoftware/Elicit/releases). It covers
  installing Survey and Admin with and without Docker Compose, identity, branding, translations,
  custom reports, securing the site, and every configuration setting. Author has an installation
  manual of its own.
- **Upgrading** an existing installation: [`DeploymentScript.md`](DeploymentScript.md).
- **Building from source** (contributors only): clone this repository with git;
  `cloneAllProjects.sh` then clones the module
  repositories and `buildDockerImages.sh` builds the images, after which `docker compose up -d`
  runs your own builds with `docker-compose.yml`, the development stack (translations, brand,
  SFTP server, database on port 5452). See
  [`docs/manual/README.md`](docs/manual/README.md) for building the manual.

<p align="right">(<a href="#readme-top">back to top</a>)</p>
<!-- ROADMAP
## Roadmap
- [ ] Feature 1
- [ ] Feature 2
- [ ] Feature 3
    - [ ] Nested Feature
 -->

See the [open issues](https://github.com/ElicitSoftware/Elicit/issues) for a full list of proposed features (and known issues).

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- CONTRIBUTING -->
## Contributing

Contributions are what make the open source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

If you have a suggestion that would make this better, please fork the repo and create a pull request. You can also simply open an issue with the tag "enhancement".
Don't forget to give the project a star! Thanks again!

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a pull request

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- ### Top contributors:

<a href="https://github.com/ElicitSoftware/Elicit/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=ElicitSoftware/Elicit" alt="contrib.rocks image" />
</a> -->

<!-- LICENSE -->
## License

Distributed under the PolyForm Noncommercial License 1.0.0. See `LICENSE.MD` for more information.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- ## Journal Articles

<p>Stoffel, E. M. and J. M. Carethers (2020). <a href="https://www-annualreviews-org.proxy.lib.umich.edu/doi/10.1146/annurev-med-052318-101009?url_ver=Z39.88-2003&amp;rfr_id=ori%3Arid%3Acrossref.org&amp;rfr_dat=cr_pub++0pubmed">&#8220;Current Approaches to Germline Cancer Genetic Testing.&#8221;</a> Annu Rev Med <b>71</b>: 85-102.</p> -->

<!-- CONTACT -->
## Contact

Matthew Demerath – m.demerath@elicitsoftware.com

Project Link: [https://github.com/ElicitSoftware/Elicit](https://github.com/ElicitSoftware/Elicit)

To report a security vulnerability, privately, see [SECURITY.md](SECURITY.md).

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- ACKNOWLEDGMENTS -->
## Acknowledgments

<!-- <a href="https://www.michiganmedicine.org"><img src="https://github.com/ElicitSoftware/Elicit/blob/main/images/Rogel-Cancer_Logo-Horizontal-CMYK.png" height="30"></a><br/> -->
<br/>
<a href="https://info.mightstudy.org"><img src="https://github.com/ElicitSoftware/Elicit/blob/main/images/MiGHT-shortlogo.png" height="50"></a><br/>
<br/>

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->
[contributors-shield]: https://img.shields.io/github/contributors/ElicitSoftware/Elicit.svg?style=for-the-badge
[contributors-url]: https://github.com/ElicitSoftware/Elicit/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/ElicitSoftware/Elicit.svg?style=for-the-badge
[forks-url]: https://github.com/ElicitSoftware/Elicit/network/members
[stars-shield]: https://img.shields.io/github/stars/ElicitSoftware/Elicit.svg?style=for-the-badge
[stars-url]: https://github.com/ElicitSoftware/Elicit/stargazers
[issues-shield]: https://img.shields.io/github/issues/ElicitSoftware/Elicit.svg?style=for-the-badge
[issues-url]: https://github.com/ElicitSoftware/Elicit/issues
[license-shield]: https://img.shields.io/github/license/ElicitSoftware/Elicit.svg?style=for-the-badge
[license-url]: https://github.com/ElicitSoftware/Elicit/blob/master/LICENSE.txt
[linkedin-shield]: https://img.shields.io/badge/-LinkedIn-black.svg?style=for-the-badge&logo=linkedin&colorB=555
[linkedin-url]: https://linkedin.com/in/linkedin_username
[product-screenshot]: images/screenshot.png
[Quarkus.io]: https://img.shields.io/badge/quarkus-000000?style=for-the-badge&logo=quarkus&logoColor=white
[Quarkus-url]: https://quarkus.io/
[Vaadin.com]: https://img.shields.io/badge/Vaadin-20232A?style=for-the-badge&logo=vaadin&logoColor=61DAFB
[Vaadin-url]: https://vaadin.com/
[Postgresql.com]: https://img.shields.io/badge/postgresql-white?style=for-the-badge&logo=postgresql&logoColor=blue
[Postgresql-url]: https://postgresql.org/
[Docker.com]: https://img.shields.io/badge/docker-257bd6?style=for-the-badge&logo=docker&logoColor=white
[Docker-url]: https://docker.com
[Java]: https://img.shields.io/badge/Java-3a75b0?style=for-the-badge&logo=openjdk&logoColor=white
[Java-url]: https://dev.java/
[Maven.org]:https://img.shields.io/badge/MAVEN-000000?style=for-the-badge&logo=apachemaven&logoColor=blue
[Maven-url]: https://maven.apache.org/
