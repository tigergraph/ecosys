import os
import argparse
import pandas as pd
from tqdm import tqdm
from datasketch import MinHash, MinHashLSH

# Default file paths
DEFAULT_INTERACTION_FILE = "data/implicit_lf_dataset.csv"
DEFAULT_OUTPUT_FILE = "data/similar_songs.csv"

# MinHash parameters
NUM_PERM = 128  # MinHash signature length
LSH_THRESHOLD = 0.05  # Jaccard similarity threshold


def load_data(file_path):
    """Load user-song interaction data."""
    df = pd.read_csv(
        file_path, sep="\t", header=None, names=["user_id", "song_id", "interaction"]
    )
    return df.groupby("song_id")["user_id"].apply(set).to_dict()


def compute_minhash(user_set):
    """Generate MinHash signature for a set of users."""
    m = MinHash(num_perm=NUM_PERM)
    for user in user_set:
        m.update(str(user).encode("utf8"))
    return m


def compute_jaccard(set1, set2):
    """Compute Jaccard similarity between two sets."""
    return len(set1 & set2) / len(set1 | set2)


def generate_minhash_signatures(song_to_users):
    """Compute MinHash signatures for songs."""
    lsh = MinHashLSH(threshold=LSH_THRESHOLD, num_perm=NUM_PERM)
    song_signatures = {}

    for song_id, user_set in tqdm(song_to_users.items(), desc="Generating MinHash"):
        m = compute_minhash(user_set)
        song_signatures[song_id] = m
        lsh.insert(str(song_id), m)

    return song_signatures, lsh


def find_similar_songs(song_signatures, song_to_users, lsh, threshold=0.05):
    """Find similar songs using LSH and compute similarity scores."""
    similar_pairs = []
    song_to_users = {str(k): v for k, v in song_to_users.items()}  # Ensure keys are str

    for song_id, m in tqdm(song_signatures.items(), desc="Finding similar songs"):
        similar_songs = lsh.query(m)  # Retrieve similar song IDs
        for other_song_id in similar_songs:
            if (
                str(song_id) > str(other_song_id)
                and str(other_song_id) in song_to_users
            ):
                score = compute_jaccard(
                    song_to_users[str(song_id)], song_to_users[str(other_song_id)]
                )
                if score > threshold:
                    similar_pairs.append((int(song_id), int(other_song_id), score))

    return similar_pairs


def save_results(similar_pairs, output_file):
    """Save similar song pairs with similarity scores to CSV."""
    os.makedirs(
        os.path.dirname(output_file), exist_ok=True
    )  # Ensure output directory exists

    df = pd.DataFrame(
        similar_pairs, columns=pd.Index(["song_id_1", "song_id_2", "similarity_score"])
    )
    df.to_csv(output_file, index=False)
    print(f"Saved {len(similar_pairs)} similar song pairs to {output_file}")


def main():
    """Main function to execute the MinHash similarity pipeline."""
    parser = argparse.ArgumentParser(
        description="Find similar songs using MinHash and LSH."
    )
    parser.add_argument(
        "--input_file",
        type=str,
        default=DEFAULT_INTERACTION_FILE,
        help="Path to the user-song interaction dataset.",
    )
    parser.add_argument(
        "--output_file",
        type=str,
        default=DEFAULT_OUTPUT_FILE,
        help="Path to save the output CSV file.",
    )
    parser.add_argument(
        "--threshold",
        type=str,
        default=0.05,
        help="Threshold for similarity score. Pairs with scores below this value will be filtered out.",
    )

    args = parser.parse_args()

    print("Loading dataset...")
    song_to_users = load_data(args.input_file)

    print("Computing MinHash signatures...")
    song_signatures, lsh = generate_minhash_signatures(song_to_users)

    print("Finding similar songs...")
    similar_pairs = find_similar_songs(
        song_signatures, song_to_users, lsh, args.threshold
    )

    print("Saving results...")
    save_results(similar_pairs, args.output_file)


if __name__ == "__main__":
    main()
