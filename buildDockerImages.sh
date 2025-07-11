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
echo "***************************************************************************"
echo "                            Building PREMM5                             "
echo "***************************************************************************"
cd ../PREMM5
./buildDockerImage.sh
echo "***************************************************************************"
echo "                            Building Pedigree                             "
echo "***************************************************************************"
cd ../Pedigree
./buildDockerImage.sh
echo "***************************************************************************"
echo "                  Building Postgres Database                  "
echo "***************************************************************************"
cd ../postgresql
./buildDockerImage.sh
echo "***************************************************************************"
echo "                            Building Admin                                 "
echo "***************************************************************************"
cd ../Admin
./buildDockerImage.sh
cd ../
echo "***************************************************************************"
echo "                               Finished                                    "
echo "***************************************************************************"