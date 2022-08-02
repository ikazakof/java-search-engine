<h1 align="center">java-search-engine</h1>

<p>The Java search engine is designed for multi-threaded indexing of a given group of sites with subsequent search by their content (Russian words).</p>
<p>The optimal speed of the program is ensured by:</p>
<li>Performing indexing process of each site/page in a separate thread</li> 
<li>Using of ForkJoinPool for recursive crawling of the site and lemmatization of its pages.</li>
<br>
<p>Search engine developed on stack of technology:<p>

<li>Syntax - Java 11</li>
<li>Framework - Springframework</li>
<li>Database - MySQL 8.0.26</li>
<li>Library - Russianmorphology 1.5</li>
<li>Library - JSOUP 1.20.2</li>
<li>Library - Lombok 1.18.24</li>
<li>Library - Json-simple 1.1.1</li>
<li>FrontEnd - HTML, CSS, JavaScript</li>

<h2 align="left">Try live DEMO</h2>
<h3><a href=http://212.193.49.63:8080>Open live demo</a> and go to "Indexing and search" chapter, point 2.</h3>
<ol>
    <li>Hosting:
    <p><a href=https://simplecloud.ru>Simple Cloud</a></p>
    </li>
    <li>Server characteristics:
    <p>Processor: 1 core 2Ghz;</p>
    <p>Memory: 2 Gb;</p>
    <p>SSD: 20 GB.</b>
    </li>
    </ol>

<h2 align="left">Prepare and start project on your device</h2>
<ol>
<li>Install prerequisites:
<p>Install MySQL 8.0.26 or later.</p>
</li>
<li>Clone repository.</li>
<li>Configure application.yml:
<p>Type username and password for connect to database with corresponding rights;</p>
<p>Type sites url and name.</p>
<p>Type the maximum percentage of the appearance of the Lema from the total number of pages in the search. <b>DEFAULT = 60%</b></p>
</li>
<li>Configure your IDE:
<p>Increase Xmx memory in VM options: -Xmx4096m;</p>
<p>Attach project directory "lib" with Russianmorphology in Project Settings -> Libraries;</p>
<p>Start Main method after maven download all project depencies.</p>
</li>
</ol>

<h2 align="left">Indexing and search</h2>
<ol>
<li>Open Search engine start page in browser - <a href=http://localhost:8080>http://localhost:8080</a>
</li>
<li>Go to <b>management</b> tab and click the "Start indexing" button;
<p align="center">
<img src="https://media.giphy.com/media/BQ1PKKds5zxrc7Gle4/giphy.gif"></p>
<p><b>ATTENTION!</b><br>
In this implementation, when you start a full indexing, all previous data will be deleted!
</p>
</li>
<li>On <b>dashboard</b> tab, you can monitor the progress of indexing;
</li>
<li>On <b>search</b> tab, you can enter a search query once any of the sites have been indexed;
</li>
<p>If there are more than 10 results, click "show more"</p>
<p align="center">
<img src="https://media.giphy.com/media/RPmxRDaOIKcJvyJ4TR/giphy.gif"></p>
</ol>

<h2 align="left">Indexing a specific page</h2>
<ol>
<li>On <b>management</b> tab, type the page url and click the "Add/update" button;
<p><b>NOTE</b><br>
Page must be member of one target site.
</p>
</li>
<li>Check the result on <b>search</b> tab.
<p align="center">
<img src="https://media.giphy.com/media/o0xCsP3HgxTZJUBAg6/giphy.gif"></p>
</li>
</ol>

