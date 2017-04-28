import React from 'react';
import GeneralListComponent from './GeneralListComponent.jsx';

class ProjectEntryList extends GeneralListComponent {
    constructor(props){
        super(props);
        this.endpoint = '/project';
        this.columns = [
            {
                header: "Id",
                key: "id",
                defaultSorting: "desc",
                dataProps: { className: 'align-right'},
                headerProps: { className: 'dashboardheader'}
            },
            GeneralListComponent.standardColumn("File association id","fileAssociationId"),
            GeneralListComponent.standardColumn("Project type", "projectTypeId"),
            GeneralListComponent.standardColumn("Created", "created"),
            GeneralListComponent.standardColumn("Owner","user"),
        ];
    }

    gotDataCallback(result){
        super.gotDataCallback(result);
    }
}

export default ProjectEntryList;