import shutil
import os

# Define source and destination folders
source_folder = r"C:\Users\sbrennan\workspace\gbacktester\src\main\resources\data\all"
destination_folder = r"C:\Users\sbrennan\workspace\gbacktester\src\main\resources\data\aa"

# List of CSV filenames to move (without .csv extension)
files_to_move = {
    "EGF", "ELVN", "ESGL", "FTDS", "GLBL", "GOOX", "GRFX", "LDWY", "LKOR",
    "PSDM", "PUTD", "RCAT", "RENB", "TLSI", "TYGO", "USFI", "VSTE", "WNTR", "XJR"
}

# Loop through files in source folder
for filename in os.listdir(source_folder):
    # Check if the filename (without extension) is in the list
    if filename.endswith(".csv") and filename[:-4] in files_to_move:
        source_path = os.path.join(source_folder, filename)
        destination_path = os.path.join(destination_folder, filename)

        # Move the file
        shutil.move(source_path, destination_path)
        print(f"Moved: {filename}")

print("Selected CSV files have been moved successfully.")
