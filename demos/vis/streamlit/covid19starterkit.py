import pyTigerGraph as tg
import streamlit as st
import pandas as pd
import flat_table
import altair as alt
import plotly.figure_factory as ff
from bokeh.plotting import figure
import plotly.express as px
import plotly.graph_objects as go


graph = tg.TigerGraphConnection(
    host="https://f82f2c67cbfc46aa8e43a89d705a0b0e.i.tgcloud.io", 
    graphname="MyGraph",
    apiToken="d1lbcf9ib4uocd7q6im769o01jv9ed4l") # make a connection to TigerGraph Box

st.title('Dynamically Visualize South Korea COVID-19 data using TigerGraph and Streamlit')

min_age, max_age = st.slider("Select Age Range", 0, 104, [10, 20])
sex = st.multiselect('Sex', ['male', 'female'])


results = graph.runInstalledQuery("streamlit")

df = pd.DataFrame(results[0]["s2"])

data = flat_table.normalize(df) # Cleaning uo the data
data = data[['v_id', 'attributes.Age', 'attributes.Sex', 'attributes.Location.latitude', 'attributes.Location.longitude']] 
if(len(sex)==1): # Filtering the data based on the sex filter input
    data = data[data['attributes.Sex']==sex[0]] 

data = data[data['attributes.Age'].between(left=min_age, right=max_age)] # Filtering the data based on age input

# grabbing location data for map 
locations = data[['attributes.Location.latitude', 'attributes.Location.longitude']] 
locations = locations.rename({'attributes.Location.latitude': 'lat', 'attributes.Location.longitude': 'lon'}, axis=1)
st.map(locations) # Using the streamlit map widget with locations input

gender_data = data['attributes.Sex']
age = data['attributes.Age']
s = age.value_counts()
age = pd.DataFrame({'Age':s.index, 'Count':s.values})

st.write(data)
st.write('Bar chart of Male and Females')
st.bar_chart(gender_data)

st.write('Line chart of Age')

fig = px.scatter(age, x="Age", y="Count")
st.plotly_chart(fig)
