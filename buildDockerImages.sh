#!/bin/bash
echo "Build all projects in Elicit"
echo "***************************************************************************"
echo "                            Building Survey                                "
echo "***************************************************************************"
cd Survey
./buildDockerImage.sh
echo "***************************************************************************"
echo "                   Building Family Health History Survey                   "
echo "***************************************************************************"
cd ../FHHS
./buildDockerImage.sh
# PREMM5 is not cloned by cloneAllProjects.sh and is commented out of
# docker-compose.yml. Re-enable this block if you restore that module.
# echo "***************************************************************************"
# echo "                            Building PREMM5                             "
# echo "***************************************************************************"
# cd ../PREMM5
# ./buildDockerImage.sh
echo "***************************************************************************"
echo "                            Building Pedigree                             "
echo "***************************************************************************"
cd ../Pedigree
./buildDockerImage.sh
# The postgresql/ directory holds only the local PGDATA volume -- there is no
# build script there. The elicitsoftware/elicit_db image is pulled, not built.
echo "***************************************************************************"
echo "                            Building Admin                                 "
echo "***************************************************************************"
cd ../Admin
./buildDockerImage.sh
echo "***************************************************************************"
echo "                            Building Author                                "
echo "***************************************************************************"
cd ../Author
./buildDockerImage.sh
cd ../
echo "***************************************************************************"
echo "                               Finished                                    "
echo "***************************************************************************"
