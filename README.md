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
  <a href="https://github.com/elicitsoftware/elicit">
    <img src="images/stacked.png" alt="Logo" width="15%" >
  </a>
<h3 align="center">Elicit Software</h3>
  <p>Copyright © 2025 The Regents of the University of Michigan</p>
</div>
<!-- TABLE OF CONTENTS -->
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li><a href="#built-with">Built With</a></li>
    <li><a href="#getting-started">Getting Started</a></li>
    <li><a href="#usage">Usage</a></li>
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
    The Elicit Software was designed and developed by <a href="https://www.michiganmedicine.org/">Michigan Medicine</a>. It is a modular system to build an deliver complex surveys with custom reporting. <br/>
    The modules are:
    <ul>
    <li><a href=https://github.com/ElicitSoftware/Admin/>Admin</a><br/>
      <ul>
      <li>Expected Release in Summer 2025</li>
      <li>The Admin application is used by the survey administrators to register subjects, view survey progress and view final reports.</li>
      </ul>
    </li>
    <li><a href=https://github.com/ElicitSoftware/Author/>Author</a><br/>
      <ul>
        <li>Expected Release in Winter 2025/26</li>
        <li>This tool will allow users to design new surveys with:</li>
        <ul>
          <li>Decision tree with branching logic to guide respondents through different survey paths based on their answers</li>
          <li>Text replacement to dynamically insert values or responses into survey questions and instructions</li>
          <li>Repeatable items to allow sections or questions to be repeated for multiple entries (e.g., family members, medications)</li>
        </ul>
      </ul>
    </li>
    <li><a href=https://github.com/ElicitSoftware/Survey/>Survey</a><br/>
      <ul>
        <li>The subject-facing application that presents questions, guides users through the decision tree, records answers, and produces final reports.</li>
      </ul>
    </li>
    <li><a href=https://github.com/ElicitSoftware/FHHS/>Family Health History Survey (FHHS)</a><br/>
      <ul>
       <li>A fully functioning Elicit Survey created to support the <a href="https://info.mightstudy.org/">Michigan Genetic Hereditary Testing Study</a></li>
       <li>The FHHS has been shared under an open source license for all cancer oncology practices to use and adapt.</li>
      </ul>
    </li>
    <li><a href=https://github.com/ElicitSoftware/Pedigree/>Pedigree</a><br/>
      <ul>
        <li>A docker container to serve the Kinship2 R package. This container supports the visual drawing of family pedigrees for the FHHS Survey </li>
      </ul>
    </li>
    <li><a href=https://github.com/ElicitSoftware/Elicit/tree/main/postgresql/>PostgreSQL Database</a><br/>
      <ul>
        <li>An open source object-relational database used for storing decision tree logic and data</li>
      </ul>
    </li>
    </ul>

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
## Getting Started
The easiest way to see Elicit Software in action is to check out the <a href=https://github.com/ElicitSoftware/FHHS/>Family Health History Survey</a>.

<!-- USAGE EXAMPLES -->
## Usage
To use the Elicit System:

1. **Download the Authoring Tool** (available Winter 2025/26).
2. **Create a New Survey** using the Authoring Tool.
3. **Publish the Survey** to the database.

Next, use the **Admin application** to:

- Register subjects.
- Generate unique tokens for each subject.
- Send invitation emails containing the survey link and token.

Subjects receive the email, follow the link, and enter their token in the Elicit Software to complete the questionnaire.

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

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- ### Top contributors:

<a href="https://github.com/ElicitSoftware/Elicit/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=ElicitSoftware/Elicit" alt="contrib.rocks image" />
</a> -->

<!-- LICENSE -->
## License

Distributed under the PolyForm Noncommercial License 1.0.0. See `LICENSE.md` for more information.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- ## Journal Articles

<p>Stoffel, E. M. and J. M. Carethers (2020). <a href="https://www-annualreviews-org.proxy.lib.umich.edu/doi/10.1146/annurev-med-052318-101009?url_ver=Z39.88-2003&amp;rfr_id=ori%3Arid%3Acrossref.org&amp;rfr_dat=cr_pub++0pubmed">&#8220;Current Approaches to Germline Cancer Genetic Testing.&#8221;</a> Annu Rev Med <b>71</b>: 85-102.</p> -->

<!-- CONTACT -->
## Contact

Matthew Demerath - m.demerath@elicitsoftware.com

Project Link: [https://github.com/ElicitSoftware/Elicit](https://github.com/ElicitSoftware/Elicit)

<p align="right">(<a href="#readme-top">back to top</a>)</p>


<!-- ACKNOWLEDGMENTS -->
## Acknowledgments

<!-- <a href="https://www.michiganmedicine.org"><img src="images/Rogel-Cancer_Logo-Horizontal-CMYK.png" height="30"></a><br/> -->
<br/>
<a href="https://info.mightstudy.org"><img src="images/MiGHT-shortlogo.png" height="50"></a><br/>
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
