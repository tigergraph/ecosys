#!/bin/bash
# gsql drop all
#gsql schema.gsql
#gsql loader.gsql
#gsql -g Recommender "DROP QUERY ALL"
gsql -g Recommender ./query/initialization.gsql
# gsql -g Recommender ./query/normalizeRating.gsql
# gsql -g Recommender ./query/training.gsql
gsql -g Recommender ./query/test.gsql
gsql -g Recommender ./query/recommend.gsql
gsql -g Recommender ./query/training_validation.gsql
gsql -g Recommender ./query/training_hybridModel.gsql
gsql -g Recommender ./query/splitData.gsql
# gsql -g Recommender ./query/training_miniBatch.gsql
# gsql -g Recommender ./query/training_biasModel.gsql
gsql -g Recommender ./query/TFIDF.gsql
gsql -g Recommender ./query/TFIDF_normalization.gsql
gsql -g Recommender ./query/user_profile_normalization.gsql
gsql -g Recommender ./query/user_profile.gsql
# gsql -g Recommender ./query/validation_TFIDF.gsql
# gsql -g Recommender ./query/validation_TFIDF_sub.gsql
gsql -g Recommender install query all


