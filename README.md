### What it does
- outputs income statistics to single encrypted PDF file
  - moving annual average, moving annual increase, moving annual total, total income

### How to get all salary slips from gmail:
1. find all emails with slips by searching for string "PREHLÁSENIE O OCHRANE SÚKROMIA A DÔVERNOSTI"
2. https://webapps.stackexchange.com/questions/102062/convenient-way-to-download-attachments-from-multiple-emails-in-gmail
3. delete files which are not slips (you can do it later - app will output names of files which cannot be parsed)

### Usage
- download bin/eset-salary-slips-1.0.jar
- required Java >= 11
- run: java -jar eset-salary-slips-1.0.jar --slips {slips}
  - {slips} is path to directory with all salary slips
  - it will ask you to enter password used to decrypt slips
  - on success, it will create file income-statistics.pdf encrypted with your password