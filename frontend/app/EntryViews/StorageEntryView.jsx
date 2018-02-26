import React from 'react';
import GenericEntryView from './GenericEntryView.jsx';

class StorageEntryView extends GenericEntryView {
    constructor(props){
        super(props);
        this.endpoint = "/api/storage"
    }

    render(){
        return <span>{this.state.content.storageType}: {this.state.content.rootpath}</span>
    }
}

export default StorageEntryView;
