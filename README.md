### What it does
- outputs income statistics to a single encrypted PDF file
  - moving annual average, moving annual increase, moving annual total, total income
  - yearly chart (values at the last month of each year): moving annual increase as bars; moving annual average,
    moving annual average adjusted for inflation (in prices of the first year in the chart),
    salary of a good / high performer (first year's average growing yearly by inflation + 3 % / + 5.5 %)
    and Slovakia inflation (HICP, 12-month average rate of change) as lines
    - inflation data is downloaded from [Eurostat](https://ec.europa.eu/eurostat/databrowser/view/prc_hicp_minr/default/table)
      (no salary data is sent) and cached in ~/.eset-salary-slips/slovak-inflation.csv; when offline, the newer
      of the cache and the bundled data (src/main/resources/slovak-inflation.csv) is used

### How to get all salary slips from gmail:
1. find all emails with slips by searching for string "PREHLÁSENIE O OCHRANE SÚKROMIA A DÔVERNOSTI"
2. https://webapps.stackexchange.com/questions/102062/convenient-way-to-download-attachments-from-multiple-emails-in-gmail
   Select all emails containing the attachments you wish to download.
   Click the three dots at the top menu and go down to "Forward as attachment" then forward all emails to yourself.
   You can then download all attachments from that one email to a folder on your hard drive. The attachments will be
   interspersed with the email files, but you can quickly delete the email files by sorting by type of file, highlighting, 
   and deleting. All attachments will be left in the folder, and you can combine them into a PDF file or easily print
   them all using the program of your choosing.
3. delete files which are not slips (you can do it later - app will output names of files which cannot be parsed)

### Usage
- download bin/eset-salary-slips-1.1.jar
- required Java >= 21
- run: java -jar eset-salary-slips-1.1.jar --slips {slips}
  - {slips} is path to directory with all salary slips
  - it will ask you to enter password used to decrypt slips
  - on success, it will create income-statistics.pdf file encrypted with your password