import React from 'react';
import GenericEntryView from './GenericEntryView.jsx';

class StorageEntryView extends GenericEntryView {
    constructor(props){
        super(props);
        this.endpoint = "/api/storage"
    }

    render(){
        if(this.state.content.nickname){
            return <span>{this.state.content.nickname} [{this.state.content.storageType}]</span>
        } else {
            const info = this.state.content.rootpath ? this.state.content.rootpath : this.state.content.device;
        return <span>{this.state.content.storageType}: {info}</span>
    }
    }
}

export default StorageEntryView;
