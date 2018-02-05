import React from 'react';
import GeneralListComponent from './GeneralListComponent.jsx';

class ProjectTypeList extends GeneralListComponent {
    constructor(props){
        super(props);
        this.endpoint = '/api/projecttype';
        this.columns = [
            {
                header: "Id",
                key: "id",
                defaultSorting: "desc",
                dataProps: { className: 'align-right'},
                headerProps: { className: 'dashboardheader'}
            },
            GeneralListComponent.standardColumn("Name","name"),
            GeneralListComponent.standardColumn("Opens with", "opensWith"),
            GeneralListComponent.standardColumn("Target version", "targetVersion")
        ];
    }

    newElementCallback(event) {
        this.props.history.push("/type/new");
    }
}

export default ProjectTypeList;