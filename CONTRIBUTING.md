# Contributing to TigerGraph Ecosystem

This repository is dedicated to provide TigerGraph's community with all the essential in-house and community developed tools to get the most value out of your TigerGraph solutions. These are hosted in [TigerGraph's Organization](https://github.com/tigergraph) on GitHub. Below are mostly guidelines, not rules. Use your best judgment, and feel free to propose changes to this document in a pull request.

#### Table Of Contents

[Code of Conduct](#code-of-conduct)

[I don't want to read this whole thing, I just have a question!!!](#i-dont-want-to-read-this-whole-thing-i-just-have-a-question)

[How Can I Contribute?](#how-can-i-contribute)
  * [Your First Code Contribution](#your-first-code-contribution)
  * [Reporting Bugs](#reporting-bugs)
  * [Suggesting Enhancements](#suggesting-enhancements)
  * [Pull Requests](#pull-requests)

[Styleguides](#styleguides)
  * [Git Commit Messages](#git-commit-messages)
  * [Documentation Styleguide](#documentation-styleguide)

[Additional Notes](#additional-notes)
  * [Issue and Pull Request Labels](#issue-and-pull-request-labels)

## Code of Conduct

This open-source TigerGraph Ecosystem and everyone participating in it is governed by the [TigerGraph Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code. Please report unacceptable behavior to [support@tigergraph.com](mailto:support@tigergraph.com).

## How Can I Contribute?

## Your First Code Contribution

Unsure where to begin contributing to TigerGraph? You can start by looking through these `beginner` and `help-wanted` issues:

* [Beginner issues][beginner] - issues which should only require a few lines of code, and a test or two.
* [Help wanted issues][help-wanted] - issues which should be a bit more involved than `beginner` issues.

Both issue lists are sorted by total number of comments. While not perfect, number of comments is a reasonable proxy for impact a given change will have.

### Reporting Bugs

This section guides you through submitting a bug report for TigerGraph. Following these guidelines helps contributors and the community understand your report :pencil:, reproduce the behavior :computer: :computer:, and find related reports :mag_right:.

Before creating bug reports, please check [this list](#before-submitting-a-bug-report) as you might find out that you don't need to create one. 

> **Note:** If you find a **Closed** issue that seems like it is the same thing that you're experiencing, open a new issue and include a link to the original issue in the body of your new one.

#### Before Submitting A Bug Report

* **Check the [FAQs on the forum](https://discuss.TigerGraph.io/c/faq)** for a list of common questions and problems.
* **Determine [which repository the problem should be reported in](https://github.com/tigergraph/ecosys/#tigergraph-ecosystem)**.
* **Perform a [cursory search](https://github.com/search?q=+is%3Aissue+user%3ATigerGraph)** to see if the problem has already been reported. If it has **and the issue is still open**, add a comment to the existing issue instead of opening a new one.

#### How Do I Submit A (Good) Bug Report?

Bugs are tracked as [GitHub issues](https://guides.github.com/features/issues/).

Explain the problem and include additional details to help contributors reproduce the problem:

* **Use a clear and descriptive title** for the issue to identify the problem.
* **Describe the exact steps which reproduce the problem** in as many details as possible. **don't just say what you did, but explain how you did it**.
* **Provide specific examples to demonstrate the steps**. Include links to files or GitHub projects, or copy/pasteable snippets, which you use in those examples. If you're providing snippets in the issue, use [Markdown code blocks](https://help.github.com/articles/markdown-basics/#multiple-lines).
* **Describe the behavior you observed after following the steps** and point out what exactly is the problem with that behavior.
* **Explain which behavior you expected to see instead and why.**


### Suggesting Enhancements

This section guides you through submitting an enhancement suggestion for TigerGraph, including completely new features and minor improvements to existing functionality. Following these guidelines helps contributors and the community understand your suggestion :pencil: and find related suggestions :mag_right:.

Before creating enhancement suggestions, please check [this list](#before-submitting-an-enhancement-suggestion) as you might find out that you don't need to create one. When you are creating an enhancement suggestion, please include as many details as possible.

#### Before Submitting An Enhancement Suggestion

* **Determine [which repository the enhancement should be suggested in](https://github.com/tigergraph/ecosys/#tigergraph-ecosystem).**
* **Perform a [cursory search](https://github.com/search?q=is%3Aissue+user%3Atigergraph)** to see if the enhancement has already been suggested. If it has, add a comment to the existing issue instead of opening a new one.

#### How Do I Submit A (Good) Enhancement Suggestion?

Enhancement suggestions are tracked as [GitHub issues](https://guides.github.com/features/issues/). After you've determined your enhancement suggestion is related to, create an issue on that repository and provide the following information:

* **Use a clear and descriptive title** for the issue to identify the suggestion.
* **Provide a step-by-step description of the suggested enhancement** in as many details as possible.
* **Provide specific examples to demonstrate the steps**. Include copy/pasteable snippets which you use in those examples, as [Markdown code blocks](https://help.github.com/articles/markdown-basics/#multiple-lines).
* **Describe the current behavior** and **explain which behavior you expected to see instead** and why.
* **Include screenshots** which help you demonstrate point out where the suggestion is related to. 
* **Explain why this enhancement would be useful** to most TigerGraph users.
* **List some other examples or applications where this enhancement exists.**

### Pull Requests

The process described here has several goals:

- Maintain TigerGraph Ecosystem quality
- Fix problems that are important to users
- Engage the community in working toward the best possible solution
- Enable a sustainable system for TG open-source Ecosystem maintainers to review contributions

Please follow these steps to have your contribution considered by the maintainers:

1. Follow all instructions in [the template](PULL_REQUEST_TEMPLATE.md)
2. Follow the [styleguides](#styleguides)
3. After you submit your pull request, a reviewer(s) may ask you to complete additional design work, tests, or other changes before your pull request can be ultimately accepted.

## Styleguides

### Git Commit Messages

* Use the present tense ("Add feature" not "Added feature")
* Use the imperative mood ("Move cursor to..." not "Moves cursor to...")
* Limit the first line to 72 characters or less
* Reference issues and pull requests liberally after the first line
* When only changing documentation, include `[ci skip]` in the commit title

### Documentation Styleguide

* Use [Markdown](https://daringfireball.net/projects/markdown).


## Additional Notes

### Issue and Pull Request Labels

This section lists the labels we use to help us track and manage issues and pull requests. Most labels are used across all TigerGraph repositories, but some are specific to `tigergraph/ecosys`.

[GitHub search](https://help.github.com/articles/searching-issues/) makes it easy to use labels for finding groups of issues or pull requests you're interested in. For example, you might be interested in [open issues across `tigergraph/ecosys` and all TigerGraph-owned packages which are labeled as bugs, but still need to be reliably reproduced](https://github.com/search?utf8=%E2%9C%93&q=is%3Aopen+is%3Aissue+user%3ATigerGraph+label%3Abug+label%3Aneeds-reproduction) or perhaps [open pull requests in `tigergraph/ecosys` which haven't been reviewed yet](https://github.com/search?utf8=%E2%9C%93&q=is%3Aopen+is%3Apr+repo%3ATigerGraph%2Fecosys+comments%3A0). To help you find issues and pull requests, each label is listed with search links for finding open items with that label in `tigergraph/ecosys` only and also across all TigerGraph repositories. We  encourage you to read about [other search filters](https://help.github.com/articles/searching-issues/) which will help you write more focused queries.

The labels are loosely grouped by their purpose, but it's not required that every issue have a label from every group or that an issue can't have more than one label from the same group.

Please open an issue on `tigergraph/ecosys` if you have suggestions for new labels, and if you notice some labels are missing on some repositories, then please open an issue on that repository.

#### Type of Issue and Issue State

| Label name | Description |
| --- | --- |
| `enhancement` | Feature requests. |
| `bug` | Confirmed bugs or reports that are very likely to be bugs. |
| `question`  | Questions more than bug reports or feature requests (e.g. how do I do X). |
| `feedback`  | General feedback more than bug reports or feature requests. |
| `help-wanted` | The TigerGraph core team would appreciate help from the community in resolving these issues. |
| `beginner` | Less complex issues which would be good first issues to work on for users who want to contribute to TigerGraph. |
| `more-information-needed` | More information needs to be collected about these problems or feature requests (e.g. steps to reproduce). |
| `needs-reproduction` | Likely bugs, but haven't been reliably reproduced. |
| `blocked` |  Issues blocked on other issues. |
| `duplicate` | Issues which are duplicates of other issues, i.e. they have been reported before. |
| `wontfix` | The TigerGraph core team has decided not to fix these issues for now, either because they're working as intended or for some other reason. |
| `invalid` | Issues which aren't valid (e.g. user errors). |
| `package-idea` |Feature request which might be good candidates for new packages, instead of extending TigerGraph or core TigerGraph packages. |
| `wrong-repo` | Issues reported on the wrong repository |

#### Pull Request Labels

| Label name | Description |
| --- | --- |
| `work-in-progress` | Pull requests which are still being worked on, more changes will follow. |
| `needs-review` | Pull requests which need code review, and approval from maintainers or TigerGraph core team. |
| `under-review` | Pull requests being reviewed by maintainers or TigerGraph core team. |
| `requires-changes` | Pull requests which need to be updated based on review comments and then reviewed again. |
| `needs-testing` | Pull requests which need manual testing. |

#### Attribution

This contributing.md was adapted from [atom/atom](https://github.com/atom/atom/blob/master/CONTRIBUTING.md).