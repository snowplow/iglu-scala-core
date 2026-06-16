addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat"    % "0.4.1")
addSbtPlugin("com.typesafe"              % "sbt-mima-plugin" % "1.1.1")
addSbtPlugin("org.scoverage"             % "sbt-scoverage"   % "2.0.4")
addSbtPlugin("org.scalameta"             % "sbt-scalafmt"    % "2.4.6")
addSbtPlugin("com.github.sbt"            % "sbt-site"        % "1.7.0")
addSbtPlugin("com.github.sbt"            % "sbt-ghpages"     % "0.9.0")
addSbtPlugin("com.eed3si9n"              % "sbt-unidoc"      % "0.4.3")
addSbtPlugin("org.scoverage"             % "sbt-coveralls"   % "1.3.2")
addSbtPlugin("com.github.sbt"            % "sbt-ci-release"  % "1.9.0")

//hack to fix sbt-site
libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
