# Workflow configuration

The way Antenna works is configured via a workflow XML file. This file describes:

* used analyzers
* optional processors
* used generators.

The file can contain variables that are replaced with values that are configured in the tool configuration (e.g. pom.xml).

Each workflow element is defined with a name and a class hint so Antenna can load it from the CLASSPATH. Additional configuration parameters can be passed to the single workflow items via configuration subsections.

The following example shows a workflow with one analyzer, one additional processor and two generators.

```xml
<!-- Controlling the Antenna workflow -->
<workflow>
    <analyzers>
        <step>
          <name>Example Analyzer</name>
          <classHint>full.qualified.class.name</classHint>
          <configuration>
            <entry key="configuration_key_1" value="configuration_value_1" />
            <entry key="configuration_key_2" value="configuration_value_2" />
          </configuration>
        </step>
    </analyzers>
    <generators>
        <step>
          <name>First Generator</name>
          <classHint>full.qualified.generator1.class.name</classHint>
       <configuration>
                  <entry key="configuration_key_1" value="configuration_value_1" />
       </configuration>
        </step>
        <step>
          <name>Second Generator</name>
          <classHint>full.qualified.generator2.class.name</classHint>
        </step>
      </generators>
      <processors>
        <step>
          <name>Arbitrary processor</name>
          <classHint>full.qualified.processor.class.name</classHint>
          <configuration>
                     <entry key="configuration_key_2" value="configuration_value_2" />
          </configuration>
        </step>
      </processors>
</workflow>
```

### Configuring default workflow steps
Antenna core is prepackaged with 2 analyzers and 4 generators:

* JSON Analyzer
* CSV Analyzer
* PDF Generator
* HTML Generator
* CSV Generator
* Source Zip Generator

However, it is quite likely that the default configuration that is provided at your organization already contains a lot more workflow steps.

A complete example on how to configure the plugin can be found in the file
```
example-projects/example-project/src/workflow.xml
```
Since the antenna tool is though to be used to enforce a company-wide standard approach to maintain open source license compliance, it allows for providing a standard workflow definition that is loaded from the classpath.

**Antenna loads  a default workflow** from the CLASSPATH at `/workflow.xml`.

The default workflow looks like this.

```
antenna-basic-assembly/antenna-basic-configuration/src/main/resources/workflow.xml
```

The default workflow contains the general steps that should be performed for a project with respect to open source compliance management but leaves out configuration that is specific for your project. For that you need to adjust the workflow for your project. E.g. it might be that some workflow steps require credentials for external systems, you want to specify custom file paths or you want to add other steps to the workflow. This has to be performed in your custom workflow configuration.

You can include another `workflow.xml` which you can configure via ``workflowDefinitionFile`` in the `pom.xml` file.

```xml
    <workflowDefinitionFile>${basedir}/customWorkflow.xml</workflowDefinitionFile>
```

If you, for example, want to add a CSV Analyzer the `customWorkflow.xml` file will look like this:

```xml
<workflow>
    <analyzers>
        <step>
            <name>CSV Analyzer</name>
            <classHint>org.eclipse.sw360.antenna.workflow.sources.analyzer.CsvAnalyzer</classHint>
            <configuration>
                <entry key="file.path" value="${basedir}/reportdata.csv" />
                <entry key="base.dir" value="${project.build.directory}/sources"/>
            </configuration>
        </step>
    </analyzers>
</workflow>
```

The effective workflow will then consist of the entries of the default workflow and the entries you specified in your workflow.

If you want to add custom workflow steps you need to have the implementation of the workflow step on the classpath. For that 
you can use standard maven means. By configuring an additional dependency for the Antenna maven plugin you can provide any further workflow steps that are available as maven dependency.
