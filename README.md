<h1>Orm</h1>
<h2>Orm(Object relational Mapping) tests for a mysql database</h2>

This is my third attempt and last for crating a framework for orm between java and MySQL

It implements One to One, One to Many and Many to Many relations.

to distinguish between a relation in the java source code you can use The Annotations @OneToOne, @OneToMany and @ManyToMany.

A package testing is included to see some demos and test the limits of this framework

<h3>Installation:</h3>
<ul>
  <li>download and add the orm.jar file to your projects classpath</li>
  <li>download and add the your mysql driver <a href="https://dev.mysql.com/downloads/connector/j/">This one works</a></li>
</ul>

<h3>Draw backs and future plans</h3>
<ul>
  <li>The biggest drawback I see in the current state is performance in future changes i will adress that</li>
  <li>Also I want to work in bidirectional relations</li>
</ul>
