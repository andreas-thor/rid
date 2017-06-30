#include <FileConstants.au3>
#include <StringConstants.au3>
#include <File.au3>
#include <MsgBoxConstants.au3>
#include <Array.au3>


$crawldir = "E:\Dev\ResearcherId\data\authors_by_topic_full\"
$topicfile = "E:\Dev\ResearcherId\data\topics.txt"


FileOpen($topicfile , 0)

For $i = 1 to _FileCountLines($topicfile )

   $line = FileReadLine($topicfile , $i)

   $page = 1

   $file = $crawldir & $line & ".topic." & $page & ".txt"

   If FileExists ($file) Then
	  Beep(500, 100)
	  ContinueLoop
   EndIf


   ; go to keyword search page
   Sleep(2000)
   MouseClick ("left", 417, 214)


   MouseClick ("left", 417, 244)
   Sleep(2000)

   ; search for keyword by typing in search box
   MouseClick ("left", 350, 574, 3)
   Sleep(2000)
   Send($line)
   Sleep(2000)
   MouseClick ("left", 475, 574)


   ; wait for 30 seconds because it takes some time
   Sleep(30000)


   Send("^a")
   Sleep(1000)
   Send("^c")
   Sleep(1000)

   FileDelete ($file)
   $fh = FileOpen($file, $FO_OVERWRITE)
   FileWrite ($fh, ClipGet ())
   FileClose ($fh)

   ; extract the number of pages
   $pages = -1
   $matchPages = StringRegExp ( ClipGet (), "Page\s+of\s+(\d+)\s+Go", $STR_REGEXPARRAYGLOBALFULLMATCH)
   If (IsArray($matchPages)) Then
	  $pagesX = $matchPages[0]
	  $pages = $pagesX[1]
   EndIf

   For $page = 2 To $pages

	  ; search for next page
	  MouseClick ("left", 863, 410, 3)
	  Sleep(2000)
	  Send($page)
	  Sleep(2000)
	  MouseClick ("left", 930, 410)	; click on go button
	  Sleep(30000)

	  Send("^a")
	  Sleep(1000)
	  Send("^c")
	  Sleep(1000)

	  $file = $crawldir & $line & ".topic." & $page & ".txt"
	  FileDelete ($file)
	  $fh = FileOpen($file, $FO_OVERWRITE)
	  FileWrite ($fh, ClipGet ())
	  FileClose ($fh)

   Next



Next
FileClose($topicfile)




#comments-start
   ; navigate to keyword by tabbing through list of top 100 keywords
   For $i = 1 To $n Step 1
	   Send("{TAB}")
	Next
	Send("{ENTER}")
#comments-end
