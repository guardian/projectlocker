import React from 'react';
import GeneralDeleteComponent from './GeneralDeleteComponent.jsx';
import SummaryComponent from '../multistep/projecttype/SummaryComponent.jsx';

class StorageDeleteComponent extends GeneralDeleteComponent {
    constructor(props){
        super(props);
        this.itemClass = "Project type";
        this.endpoint = "/api/projecttype";
    }

    getSummary(){
        return <SummaryComponent
            name={this.state.selectedItem.name}
            opensWith={this.state.selectedItem.opensWith}
            version={this.state.selectedItem.version}
        />;
    }

}

export default StorageDeleteComponent;