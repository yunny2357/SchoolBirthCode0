# Configuration file for the Sphinx documentation builder.
#
# For the full list of built-in configuration values, see the documentation:
# https://www.sphinx-doc.org/en/master/usage/configuration.html

# -- Project information -----------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#project-information

project = 'FRC 2026 Code'
copyright = '2026, Justmore5mins, baihu'
author = 'Justmore5mins, baihu'
release = '0.0.1'

# -- General configuration ---------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#general-configuration

extensions = [
    'sphinxnotes.strike',
    'sphinx_tabs.tabs'
]

templates_path = ['_templates']
exclude_patterns = []

html_title = "FRC 程式教學"
html_permalinks_icon = '<span>#</span>'

language = 'zh_TW'

html_theme = 'sphinx_book_theme'
html_static_path = ['_static']
pygments_style = "material" 

