# Config files
FrogLord has a configuration file system used for many different features due to its flexibility.  
The syntax takes inspiration from the old `.ini` file-type, but is much more flexible than `.ini`.  

## Basic Example
A common config example looks like this:
```PowerShell
[SectionName1] # All of the data seen between this line and [SectionName2] is part of a section named 'SectionName1'.
key1=123 # This could be anything, ie: a number, text, etc
key2=Hello World!
Anything after the key=value section is treated as plain text, and could be interpretted by FrogLord differently based on the context.
For example, if this configuration section was being used to configure information about a mod, the text here could be treated as the mod description.

[SectionName2] # Another section
key1=456 # [SectionName2] can have its own definition of "key1" which is completely independent of the "key1" seen in [SectionName1].
```

**Comments:**  
The `#` character is the comment character, meaning all text after `#` will be ignored, and thus it can be used to leave notes.  
To use the `#` character directly without making a comment, try escaping the value by typing `\#` instead.  

**Sections can be attached to each other.**  
When creating a new section, the number of square brances (`[` and `]`) surrounding the section name matter.  
The number of braces indicate which of the previous sections to attach the new section to.  
To attach a section to another, give the section you'd like to attach one more layer of square brackets than the one you'd like to attach to.  
If there are multiple entries with the same number of square brackets, the most recently added section will be chosen.  

For example:
```PowerShell
[ExampleSection]

[[ExampleSection2]] # This section has two square braces, meaning it is attached to [ExampleSection].

[[[ExampleSection3]]] # This section also has two square braces, meaning it is attached to [[ExampleSection2]].

[[ExampleSection4]] # This section also has two square braces, meaning it is attached to [ExampleSection].

[ExampleSection5] # This section is not attached to any other section because there is no such thing as a section with zero square brackets.  
```