# Aconso HR Document Box Sync

The [Aconso HR Document Box](https://www.aconso.com/hr-software/hr-document-box/) is a service used by some companies to make certain documents available for download for their employees.

However, as an employee the service is quite impractical:
- You can download only one document at a time (instead of all unread documents at once)
- If you download a document it will always be called "document.pdf" instead of some meaningful name 

This makes downloading and organizing the documents offline tedious.

This application "fixes" that:
- It can download all documents at once into a specified directory.
- Documents already downloaded will automatically be skipped
- It assigns meaningful filenames

This application is in no way affiliated or endorsed by Aconso.

# Running

Use `java -jar aconsosync.jar` or just click&run from your favorite file browser in order to start the GUI.

`java -jar aconsosync.jar headless [options]` can be used to run in headless mode. Running without options shows a help page with more information. 

# Usage notes

The file name pattern allows to set a custom pattern for naming files. The following variables can be used:
- `{name}` - The name of the document
- `{name:s/PATTERN/REPLACEMENT/OPTIONS}` the name of the document transformed by a regular expression. The format is intended to be similar to the format used by `sed`. Only one option is supported: `i` for case insensitive. 
- `{id}` - The file id reported by Aconso"
- `{date}` - The date of the document in format YYYY-MM-DD"
- `{date:YYYYMMDD}` - The date of the document using a custom format as used by Java's DateTimeFormatter"

A forward slash (`/`) can be used to designate directories. E.g. the pattern `{date:YYYY}/{name}.pdf` will create one directory for each year with documents and places the documents within. 
