AnimeOrganizer
==============

A small, multi-threaded Java program that monitors specified folders for
and copies them to another specified folder.

As of right now, it is specialized to working with lftp downloads and
episodes of anime, since that is the program that I use to download and
those are the files that I usually need to sort.

Uses:
* [org.json](https://github.com/stleary/JSON-java) for json parsing.
* [Apache Commons IO](https://mvnrepository.com/artifact/commons-io/commons-io/2.4)
* [AnitomyJ](https://github.com/Vorror/anitomyJ), which is a
Java port of the original [Anitomy](https://github.com/erengy/anitomy).
All credit for Anitomy and AnitomyJ goes to [@erengy](https://github.com/erengy)
and [@Vorror](https://github.com/Vorror), respectively.


## paths.json

```
{
   "paths": [
     {
       "source": "C:/example",
       "destination": "D:/example",
       "placeInSub": false
     },
     {
       "source": "C:/example2",
       "destination": "C:/example3",
       "placeInSub": true
     }
   ]
 }
 ```

Above is an example paths.json file, which is used to configure which
folder(s) to watch, and where to copy new files to. `placeInSub`
specifies where to place the file in a subfolder, which is named after
the detected file, in the destination folder.

This function is most useful for episodes of anime as
[AnitomyJ](https://github.com/Vorror/anitomyJ) will parse the
filename to determine the name of the show.


## Future Improvements

* Generalize detection for more download programs than just lftp.
* Add option to auto-rename anime downloads based on detected show and
episode number.
* Add option to move files instead of copy.