import os
import openai
import pandas as pd
from tqdm import tqdm
import argparse

# Default paths
DEFAULT_DESCRIPTIONS_PATH = "data/descriptions"
DEFAULT_OUTPUT_CSV = "data/song_embeddings.csv"


def extract_number(filename):
    """Extract numerical part from filename for proper sorting."""
    return int(filename.split(".")[0])


def read_description(file_path):
    """Read the song description from a file."""
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            return f.read().strip().replace("\n", "\\n")
    except Exception as e:
        print(f"Error reading file {file_path}: {e}")
        return None


def get_embedding(text, model="text-embedding-ada-002"):
    """Fetch embeddings from OpenAI API."""
    try:
        response = openai.embeddings.create(input=[text], model=model)
        return response.data[0].embedding
    except Exception as e:
        print(f"Error generating embedding: {e}")
        return None


def process_descriptions(descriptions_path):
    """Process all description files and generate embeddings."""
    embeddings_data = []
    description_files = sorted(os.listdir(descriptions_path), key=extract_number)

    for file in tqdm(description_files, desc="Processing descriptions"):
        item_id = file.replace(".txt", "")
        file_path = os.path.join(descriptions_path, file)

        description = read_description(file_path)
        if description:
            embedding = get_embedding(description)
            if embedding:
                embeddings_data.append((item_id, description, embedding))

    return embeddings_data


def save_embeddings(embeddings_data, output_csv):
    """Save embeddings to CSV file."""
    if not embeddings_data:
        print("No embeddings generated. Exiting.")
        return

    # Ensure output directory exists
    os.makedirs(os.path.dirname(output_csv), exist_ok=True)

    # Convert embeddings to space-separated strings
    formatted_embeddings = [" ".join(map(str, row[2])) for row in embeddings_data]

    # Create DataFrame
    df = pd.DataFrame(
        {
            "item_id": [row[0] for row in embeddings_data],
            "description": [row[1] for row in embeddings_data],
            "embedding": formatted_embeddings,
        }
    )

    # Save to CSV
    df.to_csv(output_csv, index=False)
    print(f"Embeddings saved to {output_csv}")


def main():
    """Main function to execute the embedding pipeline."""
    parser = argparse.ArgumentParser(
        description="Generate embeddings for song descriptions."
    )
    parser.add_argument(
        "--descriptions_path",
        type=str,
        default=DEFAULT_DESCRIPTIONS_PATH,
        help="Path to the descriptions folder.",
    )
    parser.add_argument(
        "--output_csv",
        type=str,
        default=DEFAULT_OUTPUT_CSV,
        help="Path to save the output CSV file.",
    )

    args = parser.parse_args()

    if not os.path.exists(args.descriptions_path):
        print(f"Error: Descriptions folder '{args.descriptions_path}' not found.")
        return

    print("Starting embedding generation process...")
    embeddings_data = process_descriptions(args.descriptions_path)
    save_embeddings(embeddings_data, args.output_csv)


if __name__ == "__main__":
    main()
