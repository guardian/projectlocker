import React from 'react';
import GeneralDeleteComponent from './GeneralDeleteComponent.jsx';
import SummaryComponent from '../multistep/storage/SummaryComponent.jsx';

class StorageDeleteComponent extends GeneralDeleteComponent {
    constructor(props){
        super(props);
        this.itemClass = "Storage";
        this.endpoint = "/api/storage";
    }

    getSummary(){
        return <SummaryComponent
            name={this.state.selectedItem.storageType}
            loginDetails={{
                user: this.state.selectedItem.user,
                host: this.state.selectedItem.host,
                port: this.state.selectedItem.port,
                password: this.state.selectedItem.password
            }}
            subfolder={this.state.selectedItem.rootpath}
        />;
    }

}

export default StorageDeleteComponent;