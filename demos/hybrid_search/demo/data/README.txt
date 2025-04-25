Knwoledge Graph Music Recommendation Dataset

Number of items: 8,640
Number of users: 5,199
Number of items-users interactions: 751,531

All the data comes from songfacts.com and last.fm websites. Items are songs, which are described in terms of textual description extracted from songfacts.com, and tags from last.fm.


Files and folders in the dataset:

/descriptions
In this folder there is one file per item with the textual description of the item. The name of the file is the id of the item plus the ".txt" extension

/tags
In this folder there is one file per item with the tags of the item separated by spaces. Multiword tags are separated by -. The name of the file is the id of the item plus the ".txt" extension. Not all items have tags, there are 401 items without tags information.

implicit_lf_dataset.txt
This file contains the interactions between users and items. There is one line per interaction (a user that downloaded a sound in this case) with the following format, fields in one line are separated by tabs:

user_id \t sound_id \t 1 \n


Scientific References

For more details on how these files were generated, we refer to the following scientific publication. We would highly appreciate if scientific publications of works partly based on this dataset quote the following publication:

Sergio Oramas, Vito Claudio Ostuni, Tommaso Di Noia, Xavier Serra and Eugenio Di Sciascio. Sound and Music Recommendation with Knowledge Graphs. ACM Transactions on Intelligent Systems and Technology (TIST), 2016