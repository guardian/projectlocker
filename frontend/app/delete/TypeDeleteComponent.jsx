import React from 'react';
import GeneralDeleteComponent from './GeneralDeleteComponent.jsx';
import SummaryComponent from '../multistep/projecttype/SummaryComponent.jsx';
import axios from 'axios';

class StorageDeleteComponent extends GeneralDeleteComponent {
    constructor(props){
        super(props);
        this.itemClass = "Project type";
        this.endpoint = "/api/projecttype";
        this.state = {
            selectedItem: {},
            postrunActions: [],
            selectedPostruns: []
        }
    }

    postDownload(){
        Promise.all([axios.get("/api/postrun"),axios.get("/api/projecttype/" + this.props.match.params.itemid + "/postrun")])
            .then(responses=>this.setState({postrunActions: responses[0].data.result, selectedPostruns: responses[1].data.result}))
            .catch(error=>console.error(error))
    }

    getSummary(){
        return <SummaryComponent
            name={this.state.selectedItem.name}
            opensWith={this.state.selectedItem.opensWith}
            version={this.state.selectedItem.version}
            fileExtension={this.state.selectedItem.fileExtension}
            plutoType={this.state.selectedItem.plutoType}
            plutoSubtype={this.state.selectedItem.plutoSubtype}
            postrunActions={this.state.postrunActions}
            selectedPostruns={this.state.selectedPostruns}
        />;
    }

}

export default StorageDeleteComponent;