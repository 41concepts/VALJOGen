# Remove footer as it is supplied by our layout instead.
class FooterRemoveMarkdownFilter < Nanoc::Filter
  identifier :footer_remove

  def run(content, params={})
    content.gsub(/^\/[^\r\n]+$/, '')
  end
end

# Enclose jumbotron named anchors with correct bootstrap markup for an jumbotron area.
class JumbotronEmbraceHtmlFilter < Nanoc::Filter
  identifier :jumbotron

  def run(content, params={})
    content=content.sub('<p><a name="jumbotron-start"></a></p>', '<div class="jumbotron">')
    content=content.sub('<p><a name="jumbotron-end"></a></p>', '</div>')
    content
  end
end

# Links are designed to work for github repository files but for the homepage they are named differently and positioned in the same dir. Thus our .md files becomes local html files instead and relative links to sources become absolute. 
class FixLinksHtmlFilter < Nanoc::Filter
  identifier :fixlinks

  def run(content, params={})
    content=content.gsub(/<a\shref="([\w]*).(m|M)(d|D)">/, '<a href="\\1.html">')
    content=content.gsub(/<a\shref=".+valjogen-processor\/README\.(html|md)">/, '<a href="processor-README.html">')
    content=content.gsub(/<a\shref=".+valjogen-examples\/README\.(html|md)">/, '<a href="examples-README.html">')
    content=content.gsub(/<a\shref=".+valjogen-integrationtests\/README\.(html|md)">/, '<a href="integrationtests-README.html">')
    content=content.gsub(/<a\shref=".+valjogen-annotations\/README\.(html|md)">/, '<a href="annotations-README.html">')
    content=content.gsub('<a href="src/main/java/com/fortyoneconcepts/valjogen/examples">', '<a href="http://github.com/41concepts/VALJOGen/tree/master/valjogen-examples/src/main/java/com/fortyoneconcepts/valjogen/examples">')
    content=content.gsub('<a href="valjogen-annotations/src/main/java/com/fortyoneconcepts/valjogen/annotations">', '<a href="http://github.com/41concepts/VALJOGen/tree/master/valjogen-annotations/src/main/java/com/fortyoneconcepts/valjogen/annotations">')
    content
  end
end